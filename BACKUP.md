# Datenbank-Backup

Die Anwendung erstellt automatisch tägliche Backups der H2-Datenbank.

## Funktionsweise

### Automatische Backups

- **Zeitpunkt**: Täglich um 2:00 Uhr nachts
- **Speicherort**:
  - Development: `./backup/`
  - Production: `/data/backup/`
- **Dateiformat**: `contactdb-backup-YYYY-MM-DD_HH-mm-ss.zip`
- **Aufbewahrung**: Die letzten 30 Tage werden automatisch gesichert
- **Automatische Bereinigung**: Backups älter als 30 Tage werden automatisch gelöscht

### Dateiname-Format

```
contactdb-backup-2025-12-28_02-00-00.zip
                 └─ Zeitstempel: Jahr-Monat-Tag_Stunde-Minute-Sekunde
```

## Konfiguration

Die Backup-Funktion kann über die Konfigurationsdateien angepasst werden:

### application.yml (Development)

```yaml
app:
  backup:
    enabled: true              # Backup aktivieren/deaktivieren
    directory: ./backup        # Backup-Verzeichnis
    cron: "0 0 2 * * *"       # Zeitplan (Cron-Expression)
```

### application-prod.yml (Production)

```yaml
app:
  backup:
    enabled: true
    directory: /data/backup
    cron: "0 0 2 * * *"
```

### Cron-Expression Beispiele

- `0 0 2 * * *` - Täglich um 2:00 Uhr
- `0 0 */6 * * *` - Alle 6 Stunden
- `0 0 0 * * SUN` - Jeden Sonntag um Mitternacht
- `0 30 1 * * *` - Täglich um 1:30 Uhr

Format: `Sekunde Minute Stunde Tag Monat Wochentag`

## Manuelles Backup

Ein manuelles Backup kann über die Service-Methode erstellt werden:

```java
@Autowired
private DatabaseBackupService backupService;

public void createManualBackup() throws IOException, SQLException {
    Path backupFile = backupService.createBackup();
    System.out.println("Backup created: " + backupFile);
}
```

## Backup-Wiederherstellung

### H2-Backup wiederherstellen

1. **Anwendung stoppen**
   ```bash
   # Application beenden
   ```

2. **Backup-Datei entpacken**
   ```bash
   unzip contactdb-backup-2025-12-28_02-00-00.zip -d restore_temp/
   ```

3. **Datenbank-Dateien ersetzen**

   **Development (In-Memory):**
   - Backup kann nicht direkt wiederhergestellt werden
   - Datenbank wird bei jedem Start neu erstellt

   **Production (File-based):**
   ```bash
   # Alte Datenbank sichern
   mv /data/contact_manager.mv.db /data/contact_manager.mv.db.old

   # Wiederhergestellte Datenbank kopieren
   cp restore_temp/contact_manager.mv.db /data/contact_manager.mv.db

   # Berechtigungen setzen
   chown appuser:appuser /data/contact_manager.mv.db
   ```

4. **Anwendung starten**
   ```bash
   ./start-production.sh
   ```

### Alternative: SCRIPT TO Wiederherstellung

Für Production mit verschlüsselter Datenbank:

```bash
# 1. Backup entpacken
unzip contactdb-backup-2025-12-28_02-00-00.zip

# 2. SQL-Script extrahieren (falls vorhanden)
# H2 Backups enthalten die Datenbankdateien direkt

# 3. Neue Datenbank aus Backup erstellen
java -cp h2*.jar org.h2.tools.Restore \
  -file contactdb-backup-2025-12-28_02-00-00.zip \
  -db /data/contact_manager \
  -cipher AES \
  -password "changeme change-encryption-key"
```

## Backup-Überwachung

### Logs überprüfen

```bash
# Development
tail -f logs/spring.log | grep -i backup

# Production
journalctl -u contact-manager -f | grep -i backup
```

### Typische Log-Ausgaben

```
INFO  DatabaseBackupService - Starting scheduled database backup
INFO  DatabaseBackupService - Created backup directory: /data/backup
INFO  DatabaseBackupService - Database backup created: /data/backup/contactdb-backup-2025-12-28_02-00-00.zip (size: 1048576 bytes)
INFO  DatabaseBackupService - Cleaning up backups older than 30 days (before 2025-11-28_02-00-00)
INFO  DatabaseBackupService - Deleted old backup: contactdb-backup-2025-11-20_02-00-00.zip
INFO  DatabaseBackupService - Scheduled backup completed successfully
```

### Fehlerbehandlung

Bei Fehlern werden Error-Logs geschrieben:

```
ERROR DatabaseBackupService - Scheduled backup failed
java.sql.SQLException: Backup failed
...
```

**Mögliche Ursachen:**
- Keine Schreibrechte im Backup-Verzeichnis
- Nicht genügend Festplattenspeicher
- Datenbank-Verbindungsprobleme

## Backup-Größe

Typische Backup-Größen:
- Kleine Datenbank (< 100 Kontakte): 50-100 KB
- Mittlere Datenbank (100-1000 Kontakte): 100-500 KB
- Große Datenbank (> 1000 Kontakte): 500 KB - 5 MB

Die Backups werden als ZIP-Dateien komprimiert gespeichert.

## Best Practices

### Empfohlene Backup-Strategie

1. **Lokale Backups**: 30 Tage (automatisch)
2. **Externe Backups**: Wöchentlich auf externen Speicher kopieren
3. **Cloud-Backups**: Monatlich in Cloud-Storage sichern

### Externe Backup-Kopie erstellen

```bash
# Wöchentliches Backup auf externen Speicher
0 0 3 * * SUN rsync -av /data/backup/ /mnt/external_backup/contact_manager/
```

### Backup-Tests durchführen

Regelmäßig (z.B. monatlich) ein Backup-Restore testen:

1. Test-Umgebung aufsetzen
2. Backup wiederherstellen
3. Anwendung starten und Daten überprüfen

## Backup deaktivieren

Falls Backups nicht gewünscht sind (z.B. in Entwicklungsumgebung):

```yaml
app:
  backup:
    enabled: false
```

## Troubleshooting

### Problem: Backup-Verzeichnis kann nicht erstellt werden

**Lösung:**
```bash
# Verzeichnis manuell erstellen
mkdir -p /data/backup

# Berechtigungen setzen
chown appuser:appuser /data/backup
chmod 755 /data/backup
```

### Problem: Zu viele Backup-Dateien

**Ursache:** Automatische Bereinigung funktioniert nicht

**Lösung:**
```bash
# Manuelle Bereinigung alter Backups (älter als 30 Tage)
find /data/backup -name "contactdb-backup-*.zip" -mtime +30 -delete
```

### Problem: Backup schlägt fehl mit "Disk full"

**Lösung:**
1. Festplattenspeicher überprüfen: `df -h`
2. Alte Backups manuell löschen
3. Backup-Retention-Period reduzieren (in DatabaseBackupService.java)

## Monitoring

### Backup-Status per REST API (optional)

Falls gewünscht, kann ein API-Endpoint hinzugefügt werden:

```java
@GetMapping("/api/admin/backup/status")
public BackupStatistics getBackupStatus() {
    return backupService.getBackupStatistics();
}
```

Rückgabe:
```json
{
  "count": 30,
  "totalSizeBytes": 15728640,
  "formattedSize": "15.00 MB",
  "newestBackup": "/data/backup/contactdb-backup-2025-12-28_02-00-00.zip"
}
```
