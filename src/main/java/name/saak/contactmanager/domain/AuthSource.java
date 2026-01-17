package name.saak.contactmanager.domain;

/**
 * Authentifizierungsquelle für Benutzer.
 */
public enum AuthSource {
    /**
     * Lokale Authentifizierung mit Benutzername und Passwort aus der Datenbank.
     */
    LOCAL,

    /**
     * OAuth2-Authentifizierung über Gitea.
     */
    GITEA
}
