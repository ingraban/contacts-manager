package name.saak.contactmanager.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    private final JdbcTemplate jdbcTemplate;
    private final Path backupDirectory;

    public DatabaseBackupService(
            JdbcTemplate jdbcTemplate,
            @Value("${app.backup.directory:./backup}") String backupDir) {
        this.jdbcTemplate = jdbcTemplate;
        this.backupDirectory = Paths.get(backupDir);

        // Backup-Verzeichnis erstellen falls nicht vorhanden
        try {
            Files.createDirectories(backupDirectory);
        } catch (IOException e) {
            logger.error("Fehler beim Erstellen des Backup-Verzeichnisses", e);
        }
    }

    /**
     * Erstellt ein Backup der Datenbank.
     */
    public Path createBackup() throws BackupException {
        String timestamp = LocalDateTime.now().format(FILENAME_FORMATTER);
        String filename = "backup_" + timestamp + ".sql";
        Path backupFile = backupDirectory.resolve(filename);

        try {
            logger.info("Erstelle Datenbank-Backup: {}", backupFile);
            String sql = String.format("SCRIPT TO '%s'", backupFile.toAbsolutePath().toString());
            jdbcTemplate.execute(sql);
            logger.info("Backup erfolgreich erstellt: {}", backupFile);
            return backupFile;
        } catch (Exception e) {
            logger.error("Fehler beim Erstellen des Backups", e);
            throw new BackupException("Fehler beim Erstellen des Backups: " + e.getMessage(), e);
        }
    }

    /**
     * Stellt die Datenbank aus einem Backup wieder her.
     * WARNUNG: Löscht alle bestehenden Daten!
     */
    public void restoreBackup(Path backupFile) throws BackupException {
        if (!Files.exists(backupFile)) {
            throw new BackupException("Backup-Datei nicht gefunden: " + backupFile);
        }

        try {
            logger.info("Stelle Datenbank wieder her aus: {}", backupFile);

            // Schritt 1: Alle bestehenden Objekte löschen (Tabellen, Sequences, etc.)
            logger.info("Lösche bestehende Datenbank-Objekte...");
            jdbcTemplate.execute("DROP ALL OBJECTS");

            // Schritt 2: Backup wiederherstellen
            logger.info("Führe Backup-Script aus...");
            String sql = String.format("RUNSCRIPT FROM '%s'", backupFile.toAbsolutePath().toString());
            jdbcTemplate.execute(sql);

            logger.info("Datenbank erfolgreich wiederhergestellt");
        } catch (Exception e) {
            logger.error("Fehler beim Wiederherstellen des Backups", e);
            throw new BackupException("Fehler beim Wiederherstellen: " + e.getMessage(), e);
        }
    }

    /**
     * Gibt Informationen über die letzte Sicherung zurück.
     */
    public Optional<BackupInfo> getLastBackupInfo() {
        try (Stream<Path> files = Files.list(backupDirectory)) {
            return files
                .filter(path -> path.getFileName().toString().startsWith("backup_"))
                .filter(path -> path.getFileName().toString().endsWith(".sql"))
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
                .filter(path -> path.getFileName().toString().endsWith(".sql"))
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
