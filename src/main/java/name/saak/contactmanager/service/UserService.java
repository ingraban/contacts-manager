package name.saak.contactmanager.service;

import name.saak.contactmanager.domain.AuthSource;
import name.saak.contactmanager.domain.Role;
import name.saak.contactmanager.domain.User;
import name.saak.contactmanager.repository.RoleRepository;
import name.saak.contactmanager.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Verarbeitet OAuth2-Login: erstellt neuen User oder aktualisiert bestehenden User.
     * Neue User sind standardmäßig deaktiviert und haben keine Rollen.
     */
    public User processOAuth2Login(Long giteaUserId, String username, String email, String displayName) {
        Optional<User> existingUser = userRepository.findByGiteaUserId(giteaUserId);

        if (existingUser.isPresent()) {
            // Bestehenden User aktualisieren
            User user = existingUser.get();
            user.setUsername(username);
            user.setEmail(email);
            user.setDisplayName(displayName);
            user.setLastLogin(LocalDateTime.now());
            return userRepository.save(user);
        } else {
            // Neuen User erstellen - DEAKTIVIERT und OHNE Rollen
            User user = new User();
            user.setGiteaUserId(giteaUserId);
            user.setUsername(username);
            user.setEmail(email);
            user.setDisplayName(displayName);
            user.setAuthSource(AuthSource.GITEA);
            user.setEnabled(false); // Neu! User muss von Admin aktiviert werden

            return userRepository.save(user);
        }
    }

    /**
     * Erstellt einen neuen lokalen Benutzer.
     */
    public User createLocalUser(String username, String password, String displayName,
                                Set<Long> roleIds) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Benutzername bereits vergeben: " + username);
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password)); // Passwort verschlüsseln
        user.setDisplayName(displayName);
        user.setAuthSource(AuthSource.LOCAL);
        user.setEnabled(true);

        Set<Role> roles = roleIds.stream()
            .map(roleId -> roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Rolle nicht gefunden")))
            .collect(Collectors.toSet());
        user.setRoles(roles);

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<User> findAllUsers() {
        return userRepository.findAll(Sort.by(Sort.Direction.ASC, "username"));
    }

    @Transactional(readOnly = true)
    public Optional<User> findUserById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Aktiviert einen Benutzer.
     */
    public void enableUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("Benutzer nicht gefunden"));
        user.setEnabled(true);
        userRepository.save(user);
    }

    /**
     * Deaktiviert einen Benutzer.
     * Prüft, dass nicht der letzte aktive Admin deaktiviert wird.
     */
    public void disableUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("Benutzer nicht gefunden"));

        // Prüfen, ob mindestens ein anderer aktiver Admin existiert
        if (user.hasRole("ADMIN") && user.isEnabled()) {
            long activeAdminCount = userRepository.findAll().stream()
                .filter(u -> u.isEnabled() && u.hasRole("ADMIN"))
                .count();

            if (activeAdminCount <= 1) {
                throw new LastAdminException(
                    "Der letzte aktive Administrator kann nicht deaktiviert werden");
            }
        }

        user.setEnabled(false);
        userRepository.save(user);
    }

    /**
     * Aktualisiert das Passwort eines lokalen Benutzers.
     * Funktioniert nur für Benutzer mit AuthSource.LOCAL.
     */
    public void updatePassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("Benutzer nicht gefunden"));

        if (user.getAuthSource() != AuthSource.LOCAL) {
            throw new IllegalArgumentException(
                "Passwort kann nur für lokale Benutzer geändert werden");
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Passwort darf nicht leer sein");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * Aktualisiert die Rollen eines Users.
     * Prüft, dass mindestens ein aktiver Admin existiert.
     */
    public void updateUserRoles(Long userId, Set<Long> roleIds, Boolean enabled) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("Benutzer nicht gefunden"));

        // Neue Rollen setzen
        Set<Role> newRoles = roleIds.stream()
            .map(roleId -> roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Rolle nicht gefunden")))
            .collect(Collectors.toSet());

        // Prüfen, ob ADMIN-Rolle entfernt wird oder User deaktiviert wird
        boolean wasAdmin = user.hasRole("ADMIN");
        boolean isAdmin = newRoles.stream().anyMatch(r -> r.getName().equals("ADMIN"));
        boolean wasEnabled = user.isEnabled();
        boolean willBeEnabled = enabled != null ? enabled : wasEnabled;

        // Wenn ein aktiver Admin zu einem nicht-aktiven Admin wird
        if (wasAdmin && wasEnabled && (!isAdmin || !willBeEnabled)) {
            long activeAdminCount = userRepository.findAll().stream()
                .filter(u -> !u.getId().equals(userId)) // Aktuellen User ausschließen
                .filter(u -> u.isEnabled() && u.hasRole("ADMIN"))
                .count();

            if (activeAdminCount == 0) {
                throw new LastAdminException(
                    "Es muss mindestens ein aktiver Administrator existieren");
            }
        }

        user.setRoles(newRoles);
        // enabled sollte immer gesetzt werden (nicht null sein dank Controller)
        user.setEnabled(enabled);
        userRepository.save(user);
    }

    /**
     * Löscht einen User.
     * Prüft, dass nicht der letzte aktive Admin gelöscht wird.
     */
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("Benutzer nicht gefunden"));

        // Prüfen, ob mindestens ein anderer aktiver Admin existiert
        if (user.hasRole("ADMIN") && user.isEnabled()) {
            long activeAdminCount = userRepository.findAll().stream()
                .filter(u -> !u.getId().equals(userId))
                .filter(u -> u.isEnabled() && u.hasRole("ADMIN"))
                .count();

            if (activeAdminCount == 0) {
                throw new LastAdminException(
                    "Der letzte aktive Administrator kann nicht gelöscht werden");
            }
        }

        userRepository.deleteById(userId);
    }

    /**
     * Exception für nicht gefundene User.
     */
    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Exception wenn der letzte Admin gelöscht/deaktiviert werden soll.
     */
    public static class LastAdminException extends RuntimeException {
        public LastAdminException(String message) {
            super(message);
        }
    }
}
