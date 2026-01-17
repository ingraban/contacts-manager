package name.saak.contactmanager.config;

import name.saak.contactmanager.domain.User;
import name.saak.contactmanager.service.UserService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/**
 * Custom OAuth2UserService für Gitea-Integration.
 * Verarbeitet OAuth2-Login und synchronisiert User mit der Datenbank.
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserService userService;

    public CustomOAuth2UserService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // Lade OAuth2-Benutzerdaten von Gitea
        OAuth2User oauth2User = super.loadUser(userRequest);

        // Extrahiere Gitea-Benutzerdaten
        Long giteaUserId = oauth2User.getAttribute("id");
        String username = oauth2User.getAttribute("login");
        String email = oauth2User.getAttribute("email");
        String displayName = oauth2User.getAttribute("full_name");

        // Validierung
        if (giteaUserId == null || username == null) {
            throw new OAuth2AuthenticationException("Ungültige Benutzerdaten von Gitea");
        }

        // Erstelle oder aktualisiere User in der Datenbank
        User user = userService.processOAuth2Login(giteaUserId, username, email, displayName);

        // Prüfe, ob Benutzer aktiviert ist
        if (!user.isEnabled()) {
            throw new OAuth2AuthenticationException(
                "Ihr Benutzerkonto ist noch nicht aktiviert. " +
                "Bitte wenden Sie sich an einen Administrator.");
        }

        // Gebe CustomOAuth2User mit Datenbankrollen zurück
        return new CustomOAuth2User(oauth2User, user);
    }
}
