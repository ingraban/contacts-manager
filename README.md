# contacts-manager
Die Verwaltung von Kontakten für Grußkarten und ähnliches

## VS-Code Shortcuts

Mit `Cmd+K`, `Cmd+S` gelangt man zu den Einstellungen für Keyboard Shortcuts.

Hier kann man nach Befehlen suchen (z.B. `Debug Java`).
Dann kann man einen Shortcut hinzufügen.
Sollte dieser bereits vergeben sein, wird es angezeigt.

| Befehl | Shortcut
| -- | :--:
| Debug Java | `Cmd+J`, `Cmd+D`

## Release erstellen und ausliefern

### Release erstellen

Eigentlich müssen wir nur einen Tag mit der Version anlegen und diesen dann pushen.

```shell
$ git tag -a v0.1.0 -m "Nachricht für den Release"
$ git push origin v0.1.0
```

Jetzt sollte in GitHub ein Workflow loslaufen und einen Release bauen.
Damit wird auch ein Docker Image erstellt und auf Dockerhub gepushed.

Dieses kann dann auf dem Zielsystem eingerichtet bzw. aktualisiert werden.

## Dev / Prod Umgebung

Wir haben es mit Skripten versucht.
Die [Dokumentation](PRODUCTION.md) haben wir in einer separaten Datei abgelegt.
