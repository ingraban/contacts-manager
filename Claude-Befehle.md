# Claude Befehle

## JPA Einbauen

Erweitere das Projekt, damit wir über JPA auf eine Datenbank zugreifen können. Die Datenbank soll für den Test eine H2-Memory Datenbank mit Testdaten über ein INIT-Script sein. In der Produktion soll eine PostgreSQL verwendet werden.

Die Datenbank soll über Luiquibase aufgesetzt werden. 

Als erste Verwaltungsmaske soll ein Kontakt mit den folgenden Feldern in der Maske verwaltet werden können.

- Vorname (String)
- Nachname (String)
- Straße (String)
- Postleitzahl (String)
- Ort (String)
- Anrede (String)
- Telefon1 (String)
- Telefon2 (String)
- EMail (String)

Es soll einen Index geben, bei dem eine Volltextsuche angeboten wird. Außerdem sollen Datensätze erstellt, gelesen, geändert und gelöscht werden können.

Schreibe entsprechende Tests und führe diese durch.

1. Überarbeitung

Kannst Du die Button "Suchen" und "Zurücksetzen" in die gleiche Zeile setzen, wie den Suchstring. Außerdem solltest Du symbole (Lupe, Pfeil im Kreis verwenden). Für die "Bearbeiten" den Stift verwenden, für Löschen den Papierkorb und für neu das Plus-Zeichen

2. Überarbeitung

Verwende bitte Bootstrap Icons und stellen die textuelle beschreibung als optionale Description zur verfügung, die beim Hoverns angezeigt wird 
