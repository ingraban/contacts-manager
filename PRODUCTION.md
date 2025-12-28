# Production Deployment

## H2 Datenbank mit Verschlüsselung

Die Production-Konfiguration verwendet eine dateibasierte H2-Datenbank mit AES-Verschlüsselung.

### Konfiguration

Die Datenbankdatei wird im Verzeichnis `./data/` gespeichert:
- **Pfad**: `./data/contactdb.mv.db`
- **Verschlüsselung**: AES

### Passwörter ändern (WICHTIG!)

⚠️ **Vor dem ersten Production-Start müssen die Passwörter geändert werden!**

Bearbeiten Sie `src/main/resources/application-prod.yml`:

```yaml
spring:
  datasource:
    password: <database-password> <encryption-key>
```

**Format**: Das Passwort besteht aus zwei Teilen, getrennt durch ein Leerzeichen:
1. **Database-Password**: Passwort für den Datenbankzugriff
2. **Encryption-Key**: Schlüssel für die AES-Verschlüsselung

**Beispiel**:
```yaml
password: MySecureDbPass123 MyEncryptionKey456
```

### Anwendung starten

**Development** (Standard, In-Memory H2):
```bash
./mvnw spring-boot:run
```

**Production** (File-based H2 mit Verschlüsselung):
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

Oder als JAR:
```bash
java -jar target/contact-manager-0.0.0-SNAPSHOT.jar --spring.profiles.active=prod
```

### Datensicherung

Die Datenbankdatei befindet sich in:
```
./data/contactdb.mv.db
```

**Backup-Empfehlung**:
- Regelmäßige Sicherung der Datei `./data/contactdb.mv.db`
- Encryption-Key separat und sicher aufbewahren
- Bei Verlust des Encryption-Keys ist die Datenbank nicht wiederherstellbar!

### Unterschiede Development vs. Production

| Feature | Development | Production |
|---------|------------|------------|
| Datenbank | In-Memory (flüchtig) | File-based (persistent) |
| Verschlüsselung | Nein | Ja (AES) |
| Thymeleaf Cache | Aus | An |
| SQL Logging | An | Aus |
| H2 Console | An | Aus |
| Log Level | DEBUG | INFO/WARN |

### Erste Initialisierung

Beim ersten Start mit Production-Profil:

1. Das Verzeichnis `./data/` wird automatisch erstellt
2. Liquibase führt alle Migrations aus
3. Die Datenbank wird mit dem angegebenen Encryption-Key verschlüsselt
4. Die Datei `contactdb.mv.db` wird erstellt

### Troubleshooting

**Problem**: "Wrong user name or password"
- **Lösung**: Prüfen Sie das Passwort-Format (zwei Passwörter mit Leerzeichen getrennt)

**Problem**: Datei kann nicht geöffnet werden
- **Lösung**: Prüfen Sie Schreibrechte im `./data/` Verzeichnis

**Problem**: "Encryption error"
- **Lösung**: Der Encryption-Key stimmt nicht mit der existierenden Datenbank überein
- Bei Änderung des Keys muss die alte Datenbank gelöscht werden (Backup vorher erstellen!)
