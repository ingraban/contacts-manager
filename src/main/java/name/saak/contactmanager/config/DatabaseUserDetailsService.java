package name.saak.contactmanager.config;

import name.saak.contactmanager.domain.AuthSource;
import name.saak.contactmanager.domain.User;
import name.saak.contactmanager.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

/**
 * UserDetailsService-Implementierung, die Benutzer aus der Datenbank lädt.
 * Unterstützt nur lokale Authentifizierung (AuthSource.LOCAL).
 */
@Service("databaseUserDetailsService")
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public DatabaseUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException(
                "Benutzer nicht gefunden: " + username));

        // Nur lokale Benutzer können sich über Form-Login anmelden
        if (user.getAuthSource() != AuthSource.LOCAL) {
            throw new UsernameNotFoundException(
                "Benutzer verwendet keine lokale Authentifizierung: " + username);
        }

        // Prüfen, ob Benutzer aktiviert ist
        if (!user.isEnabled()) {
            throw new UsernameNotFoundException(
                "Benutzer ist deaktiviert: " + username);
        }

        var authorities = user.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
            .collect(Collectors.toList());

        return org.springframework.security.core.userdetails.User
            .withUsername(user.getUsername())
            .password(user.getPassword() != null ? user.getPassword() : "")
            .authorities(authorities)
            .accountExpired(false)
            .accountLocked(!user.isEnabled())
            .credentialsExpired(false)
            .disabled(!user.isEnabled())
            .build();
    }
}
