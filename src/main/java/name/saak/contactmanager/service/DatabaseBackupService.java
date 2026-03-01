package name.saak.contactmanager.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Service für Datenbank-Backup und -Wiederherstellung.
 */
@Service
public class DatabaseBackupService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseBackupService.class);
    private static final DateTimeFormatter FILENAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    // Verschlüsselungs-Konstanten
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final int SALT_LENGTH = 16;
    private static final int KEY_LENGTH = 256;
    private static final int PBKDF2_ITERATIONS = 65536;

    private final JdbcTemplate jdbcTemplate;
    private final Path backupDirectory;
    private final String encryptionPassword;

    public DatabaseBackupService(
            JdbcTemplate jdbcTemplate,
            @Value("${app.backup.directory:./backup}") String backupDir,
            @Value("${app.backup.encryption.password:}") String encryptionPassword) {
        this.jdbcTemplate = jdbcTemplate;
        this.backupDirectory = Paths.get(backupDir);
        this.encryptionPassword = encryptionPassword;

        // Backup-Verzeichnis erstellen falls nicht vorhanden
        try {
            Files.createDirectories(backupDirectory);
        } catch (IOException e) {
            logger.error("Fehler beim Erstellen des Backup-Verzeichnisses", e);
        }

        // Warnung wenn kein Verschlüsselungspasswort gesetzt ist
        if (encryptionPassword == null || encryptionPassword.trim().isEmpty()) {
            logger.warn("WARNUNG: Kein Verschlüsselungspasswort gesetzt! Backups werden UNVERSCHLÜSSELT gespeichert.");
            logger.warn("Setzen Sie die Environment-Variable BACKUP_ENCRYPTION_PASSWORD für verschlüsselte Backups.");
        } else {
            logger.info("Verschlüsselung für Backups aktiviert");
        }
    }

    /**
     * Erstellt ein Backup der Datenbank.
     * Wenn ein Verschlüsselungspasswort gesetzt ist, wird das Backup verschlüsselt.
     */
    public Path createBackup() throws BackupException {
        String timestamp = LocalDateTime.now().format(FILENAME_FORMATTER);
        String filename = "backup_" + timestamp + ".sql";
        Path backupFile = backupDirectory.resolve(filename);
        Path tempFile = null;

        try {
            // Wenn Verschlüsselung aktiviert ist, zuerst in temporäre Datei schreiben
            if (isEncryptionEnabled()) {
                tempFile = Files.createTempFile("backup_temp_", ".sql");
                logger.info("Erstelle temporäres Datenbank-Backup: {}", tempFile);
                String sql = String.format("SCRIPT TO '%s'", tempFile.toAbsolutePath().toString());
                jdbcTemplate.execute(sql);

                // Verschlüsseln und in finale Datei schreiben
                logger.info("Verschlüssele Backup...");
                Path encryptedFile = backupDirectory.resolve("backup_" + timestamp + ".enc");
                encryptBackupFile(tempFile, encryptedFile);

                // Temporäre Datei löschen
                Files.deleteIfExists(tempFile);

                logger.info("Verschlüsseltes Backup erstellt: {}", encryptedFile);
                return encryptedFile;
            } else {
                // Unverschlüsseltes Backup
                logger.info("Erstelle unverschlüsseltes Datenbank-Backup: {}", backupFile);
                String sql = String.format("SCRIPT TO '%s'", backupFile.toAbsolutePath().toString());
                jdbcTemplate.execute(sql);
                logger.info("Backup erfolgreich erstellt: {}", backupFile);
                return backupFile;
            }
        } catch (Exception e) {
            // Aufräumen im Fehlerfall
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ex) {
                    logger.error("Fehler beim Löschen der temporären Datei", ex);
                }
            }
            logger.error("Fehler beim Erstellen des Backups", e);
            throw new BackupException("Fehler beim Erstellen des Backups: " + e.getMessage(), e);
        }
    }

    /**
     * Stellt die Datenbank aus einem Backup wieder her.
     * WARNUNG: Löscht alle bestehenden Daten!
     * Unterstützt sowohl verschlüsselte (.enc) als auch unverschlüsselte (.sql) Backups.
     */
    public void restoreBackup(Path backupFile) throws BackupException {
        if (!Files.exists(backupFile)) {
            throw new BackupException("Backup-Datei nicht gefunden: " + backupFile);
        }

        Path tempFile = null;
        try {
            boolean isEncrypted = backupFile.toString().endsWith(".enc");
            Path sqlFile = backupFile;

            // Wenn verschlüsselt, zuerst entschlüsseln
            if (isEncrypted) {
                if (!isEncryptionEnabled()) {
                    throw new BackupException("Backup ist verschlüsselt, aber kein Entschlüsselungspasswort gesetzt!");
                }
                logger.info("Entschlüssele Backup...");
                tempFile = Files.createTempFile("restore_temp_", ".sql");
                decryptBackupFile(backupFile, tempFile);
                sqlFile = tempFile;
            }

            logger.info("Stelle Datenbank wieder her aus: {}", backupFile);

            // Schritt 1: Alle bestehenden Objekte löschen (Tabellen, Sequences, etc.)
            logger.info("Lösche bestehende Datenbank-Objekte...");
            jdbcTemplate.execute("DROP ALL OBJECTS");

            // Schritt 2: Backup wiederherstellen
            logger.info("Führe Backup-Script aus...");
            String sql = String.format("RUNSCRIPT FROM '%s'", sqlFile.toAbsolutePath().toString());
            jdbcTemplate.execute(sql);

            logger.info("Datenbank erfolgreich wiederhergestellt");
        } catch (BackupException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Fehler beim Wiederherstellen des Backups", e);
            throw new BackupException("Fehler beim Wiederherstellen: " + e.getMessage(), e);
        } finally {
            // Temporäre Datei aufräumen
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    logger.error("Fehler beim Löschen der temporären Datei", e);
                }
            }
        }
    }

    /**
     * Gibt Informationen über die letzte Sicherung zurück.
     */
    public Optional<BackupInfo> getLastBackupInfo() {
        try (Stream<Path> files = Files.list(backupDirectory)) {
            return files
                .filter(path -> path.getFileName().toString().startsWith("backup_"))
                .filter(path -> {
                    String name = path.getFileName().toString();
                    return name.endsWith(".sql") || name.endsWith(".enc");
                })
                .max(Comparator.comparing(path -> {
                    try {
                        return Files.getLastModifiedTime(path);
                    } catch (IOException e) {
                        return null;
                    }
                }))
                .map(this::createBackupInfo);
        } catch (IOException e) {
            logger.error("Fehler beim Lesen des Backup-Verzeichnisses", e);
            return Optional.empty();
        }
    }

    /**
     * Listet alle verfügbaren Backups auf.
     */
    public List<BackupInfo> listBackups() {
        try (Stream<Path> files = Files.list(backupDirectory)) {
            return files
                .filter(path -> path.getFileName().toString().startsWith("backup_"))
                .filter(path -> {
                    String name = path.getFileName().toString();
                    return name.endsWith(".sql") || name.endsWith(".enc");
                })
                .sorted(Comparator.comparing((Path path) -> {
                    try {
                        return Files.getLastModifiedTime(path);
                    } catch (IOException e) {
                        return null;
                    }
                }).reversed())
                .map(this::createBackupInfo)
                .toList();
        } catch (IOException e) {
            logger.error("Fehler beim Lesen des Backup-Verzeichnisses", e);
            return List.of();
        }
    }

    private BackupInfo createBackupInfo(Path backupFile) {
        try {
            return new BackupInfo(
                backupFile.getFileName().toString(),
                backupFile,
                Files.getLastModifiedTime(backupFile).toInstant(),
                Files.size(backupFile)
            );
        } catch (IOException e) {
            logger.error("Fehler beim Lesen der Backup-Informationen", e);
            return null;
        }
    }

    /**
     * Prüft ob Verschlüsselung aktiviert ist.
     */
    private boolean isEncryptionEnabled() {
        return encryptionPassword != null && !encryptionPassword.trim().isEmpty();
    }

    /**
     * Verschlüsselt eine Backup-Datei mit AES-256-GCM.
     */
    private void encryptBackupFile(Path inputFile, Path outputFile) throws Exception {
        // Salt und IV generieren
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        byte[] iv = new byte[GCM_IV_LENGTH];
        random.nextBytes(salt);
        random.nextBytes(iv);

        // Key aus Passwort ableiten
        SecretKey key = deriveKey(encryptionPassword, salt);

        // Cipher initialisieren
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);

        // Datei lesen
        byte[] plaintext = Files.readAllBytes(inputFile);

        // Verschlüsseln
        byte[] ciphertext = cipher.doFinal(plaintext);

        // Salt + IV + Ciphertext in Ausgabedatei schreiben
        byte[] output = new byte[SALT_LENGTH + GCM_IV_LENGTH + ciphertext.length];
        System.arraycopy(salt, 0, output, 0, SALT_LENGTH);
        System.arraycopy(iv, 0, output, SALT_LENGTH, GCM_IV_LENGTH);
        System.arraycopy(ciphertext, 0, output, SALT_LENGTH + GCM_IV_LENGTH, ciphertext.length);

        Files.write(outputFile, output);
    }

    /**
     * Entschlüsselt eine Backup-Datei.
     */
    private void decryptBackupFile(Path inputFile, Path outputFile) throws Exception {
        // Verschlüsselte Datei lesen
        byte[] encrypted = Files.readAllBytes(inputFile);

        if (encrypted.length < SALT_LENGTH + GCM_IV_LENGTH) {
            throw new BackupException("Ungültiges verschlüsseltes Backup: Datei zu klein");
        }

        // Salt und IV extrahieren
        byte[] salt = new byte[SALT_LENGTH];
        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(encrypted, 0, salt, 0, SALT_LENGTH);
        System.arraycopy(encrypted, SALT_LENGTH, iv, 0, GCM_IV_LENGTH);

        // Ciphertext extrahieren
        int ciphertextLength = encrypted.length - SALT_LENGTH - GCM_IV_LENGTH;
        byte[] ciphertext = new byte[ciphertextLength];
        System.arraycopy(encrypted, SALT_LENGTH + GCM_IV_LENGTH, ciphertext, 0, ciphertextLength);

        // Key aus Passwort ableiten
        SecretKey key = deriveKey(encryptionPassword, salt);

        // Cipher initialisieren
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);

        // Entschlüsseln
        byte[] plaintext = cipher.doFinal(ciphertext);

        // In Ausgabedatei schreiben
        Files.write(outputFile, plaintext);
    }

    /**
     * Leitet einen kryptographischen Schlüssel aus dem Passwort ab (PBKDF2).
     */
    private SecretKey deriveKey(String password, byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] key = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(key, "AES");
    }

    /**
     * Informationen über ein Backup.
     */
    public record BackupInfo(
        String filename,
        Path path,
        java.time.Instant timestamp,
        long sizeBytes
    ) {
        public String getFormattedSize() {
            if (sizeBytes < 1024) {
                return sizeBytes + " B";
            } else if (sizeBytes < 1024 * 1024) {
                return String.format("%.1f KB", sizeBytes / 1024.0);
            } else {
                return String.format("%.1f MB", sizeBytes / (1024.0 * 1024.0));
            }
        }

        public LocalDateTime getLocalDateTime() {
            return LocalDateTime.ofInstant(timestamp, java.time.ZoneId.systemDefault());
        }
    }

    /**
     * Exception für Backup-Fehler.
     */
    public static class BackupException extends Exception {
        public BackupException(String message) {
            super(message);
        }

        public BackupException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
