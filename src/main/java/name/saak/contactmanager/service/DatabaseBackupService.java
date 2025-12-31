package name.saak.contactmanager.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

/**
 * Service für automatische Datenbank-Backups.
 * Erstellt täglich um 2:00 Uhr ein Backup und löscht Backups älter als 30 Tage.
 */
@Service
public class DatabaseBackupService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseBackupService.class);
    private static final DateTimeFormatter BACKUP_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final int RETENTION_DAYS = 30;

    private final DataSource dataSource;
    private final String backupDirectory;
    private final boolean backupEnabled;

    public DatabaseBackupService(
            DataSource dataSource,
            @Value("${app.backup.directory:./backup}") String backupDirectory,
            @Value("${app.backup.enabled:true}") boolean backupEnabled) {
        this.dataSource = dataSource;
        this.backupDirectory = backupDirectory;
        this.backupEnabled = backupEnabled;
    }

    /**
     * Scheduled Task: Erstellt täglich um 2:00 Uhr ein Backup.
     */
    @Scheduled(cron = "${app.backup.cron:0 0 2 * * *}")
    public void createScheduledBackup() {
        if (!backupEnabled) {
            log.debug("Backup is disabled, skipping scheduled backup");
            return;
        }

        log.info("Starting scheduled database backup");
        try {
            createBackup();
            cleanupOldBackups();
            log.info("Scheduled backup completed successfully");
        } catch (Exception e) {
            log.error("Scheduled backup failed", e);
        }
    }

    /**
     * Erstellt ein Datenbank-Backup.
     *
     * @return Path zum erstellten Backup
     * @throws IOException wenn der Backup-Ordner nicht erstellt werden kann
     * @throws SQLException wenn das Backup fehlschlägt
     */
    public Path createBackup() throws IOException, SQLException {
        // Backup-Verzeichnis erstellen falls nicht vorhanden
        Path backupDir = Paths.get(backupDirectory);
        if (!Files.exists(backupDir)) {
            Files.createDirectories(backupDir);
            log.info("Created backup directory: {}", backupDir.toAbsolutePath());
        }

        // Backup-Dateiname mit Zeitstempel
        String timestamp = LocalDateTime.now().format(BACKUP_DATE_FORMAT);
        String backupFileName = String.format("contactdb-backup-%s.zip", timestamp);
        Path backupFile = backupDir.resolve(backupFileName);

        // H2 BACKUP SQL ausführen
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            String sql = String.format("BACKUP TO '%s'", backupFile.toAbsolutePath());
            statement.execute(sql);

            log.info("Database backup created: {} (size: {} bytes)",
                    backupFile.toAbsolutePath(),
                    Files.size(backupFile));

            return backupFile;
        }
    }

    /**
     * Löscht Backups, die älter als 30 Tage sind.
     */
    public void cleanupOldBackups() {
        Path backupDir = Paths.get(backupDirectory);
        if (!Files.exists(backupDir)) {
            log.debug("Backup directory does not exist, skipping cleanup");
            return;
        }

        LocalDateTime cutoffDate = LocalDateTime.now().minus(RETENTION_DAYS, ChronoUnit.DAYS);
        log.info("Cleaning up backups older than {} days (before {})",
                RETENTION_DAYS, cutoffDate.format(BACKUP_DATE_FORMAT));

        try (Stream<Path> files = Files.list(backupDir)) {
            files.filter(path -> path.toString().endsWith(".zip"))
                 .filter(path -> path.getFileName().toString().startsWith("contactdb-backup-"))
                 .forEach(path -> {
                     try {
                         LocalDateTime fileTime = Files.getLastModifiedTime(path)
                                 .toInstant()
                                 .atZone(java.time.ZoneId.systemDefault())
                                 .toLocalDateTime();

                         if (fileTime.isBefore(cutoffDate)) {
                             Files.delete(path);
                             log.info("Deleted old backup: {}", path.getFileName());
                         }
                     } catch (IOException e) {
                         log.error("Failed to delete old backup: {}", path.getFileName(), e);
                     }
                 });
        } catch (IOException e) {
            log.error("Failed to cleanup old backups", e);
        }
    }

    /**
     * Gibt Statistiken über vorhandene Backups zurück.
     */
    public BackupStatistics getBackupStatistics() {
        Path backupDir = Paths.get(backupDirectory);
        if (!Files.exists(backupDir)) {
            return new BackupStatistics(0, 0, null);
        }

        try (Stream<Path> files = Files.list(backupDir)) {
            var backups = files.filter(path -> path.toString().endsWith(".zip"))
                              .filter(path -> path.getFileName().toString().startsWith("contactdb-backup-"))
                              .toList();

            long totalSize = backups.stream()
                                   .mapToLong(path -> {
                                       try {
                                           return Files.size(path);
                                       } catch (IOException e) {
                                           return 0;
                                       }
                                   })
                                   .sum();

            Path newestBackup = backups.stream()
                                      .max((p1, p2) -> {
                                          try {
                                              return Files.getLastModifiedTime(p1)
                                                         .compareTo(Files.getLastModifiedTime(p2));
                                          } catch (IOException e) {
                                              return 0;
                                          }
                                      })
                                      .orElse(null);

            return new BackupStatistics(backups.size(), totalSize, newestBackup);
        } catch (IOException e) {
            log.error("Failed to get backup statistics", e);
            return new BackupStatistics(0, 0, null);
        }
    }

    /**
     * Backup-Statistiken.
     */
    public record BackupStatistics(int count, long totalSizeBytes, Path newestBackup) {
        public String getFormattedSize() {
            if (totalSizeBytes < 1024) {
                return totalSizeBytes + " B";
            } else if (totalSizeBytes < 1024 * 1024) {
                return String.format("%.2f KB", totalSizeBytes / 1024.0);
            } else {
                return String.format("%.2f MB", totalSizeBytes / (1024.0 * 1024.0));
            }
        }
    }
}
