package name.saak.contactmanager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Konfiguration für den PasswordEncoder.
 * Separiert von SecurityConfig, um zirkuläre Abhängigkeiten zu vermeiden.
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * PasswordEncoder für die Verschlüsselung von Passwörtern lokaler Benutzer.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
