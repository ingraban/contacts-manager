package name.saak.contactmanager.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class DatabaseBackupServiceTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private DatabaseBackupService backupService;
    private final String testBackupDir = "./test-backup";

    @BeforeEach
    void setUp() throws IOException {
        // Test-Service mit eigenem Backup-Verzeichnis erstellen
        backupService = new DatabaseBackupService(jdbcTemplate, testBackupDir);

        // Test-Verzeichnis erstellen
        Path backupPath = Paths.get(testBackupDir);
        if (!Files.exists(backupPath)) {
            Files.createDirectories(backupPath);
        }
    }

    @AfterEach
    void tearDown() throws IOException {
        // Test-Verzeichnis aufräumen
        Path backupPath = Paths.get(testBackupDir);
        if (Files.exists(backupPath)) {
            try (Stream<Path> paths = Files.walk(backupPath)) {
                paths.sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            // Ignore
                        }
                    });
            }
        }
    }

    @Test
    @DisplayName("Backup erstellen")
    void testCreateBackup() throws Exception {
        // When
        Path backupFile = backupService.createBackup();

        // Then
        assertThat(backupFile).exists();
        assertThat(backupFile.getFileName().toString()).startsWith("backup_");
        assertThat(backupFile.getFileName().toString()).endsWith(".sql");
        assertThat(Files.size(backupFile)).isGreaterThan(0);
    }

    @Test
    @DisplayName("Letzte Sicherung abrufen")
    void testGetLastBackupInfo() throws Exception {
        // Given
        backupService.createBackup();
        Thread.sleep(100); // Kurze Pause
        backupService.createBackup();

        // When
        var lastBackup = backupService.getLastBackupInfo();

        // Then
        assertThat(lastBackup).isPresent();
        assertThat(lastBackup.get().filename()).startsWith("backup_");
    }

    @Test
    @DisplayName("Backups auflisten")
    void testListBackups() throws Exception {
        // Given
        backupService.createBackup();
        Thread.sleep(1100); // Länger warten für unterschiedlichen Timestamp
        backupService.createBackup();

        // When
        var backups = backupService.listBackups();

        // Then
        assertThat(backups).hasSizeGreaterThanOrEqualTo(1);
        assertThat(backups.get(0).filename()).startsWith("backup_");
    }

    @Test
    @DisplayName("Backup wiederherstellen - Datei nicht gefunden")
    void testRestoreBackupFileNotFound() {
        // Given
        Path nonExistentFile = Paths.get(testBackupDir, "nonexistent.sql");

        // When/Then
        assertThatThrownBy(() -> backupService.restoreBackup(nonExistentFile))
            .isInstanceOf(DatabaseBackupService.BackupException.class)
            .hasMessageContaining("nicht gefunden");
    }

    @Test
    @DisplayName("Keine Backups vorhanden")
    void testNoBackupsAvailable() {
        // When
        var lastBackup = backupService.getLastBackupInfo();
        var backups = backupService.listBackups();

        // Then
        assertThat(lastBackup).isEmpty();
        assertThat(backups).isEmpty();
    }
}
