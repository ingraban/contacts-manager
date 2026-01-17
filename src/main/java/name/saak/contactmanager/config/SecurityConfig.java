package name.saak.contactmanager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	private final CustomOAuth2UserService customOAuth2UserService;

	public SecurityConfig(CustomOAuth2UserService customOAuth2UserService) {
		this.customOAuth2UserService = customOAuth2UserService;
	}

	/**
	 * Security-Konfiguration für H2 Console (nur Development).
	 * Diese Konfiguration hat höhere Priorität (Order 1) und gilt nur für /h2-console/**
	 * ALLE Sicherheits-Header werden für H2 Console deaktiviert.
	 */
	@Bean
	@Order(1)
	SecurityFilterChain h2ConsoleSecurityFilterChain(HttpSecurity http) throws Exception {
		http.securityMatcher(
				new AntPathRequestMatcher("/h2-console/**")
			)
			.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
			.csrf(csrf -> csrf.disable()) // CSRF deaktiviert
			.headers(headers -> headers.disable()); // ALLE Header-Security deaktiviert
		return http.build();
	}

	/**
	 * Haupt-Security-Konfiguration für die Anwendung mit Form Login und OAuth2.
	 * Form Login verwendet DatabaseUserDetailsService (nur für LOCAL-Benutzer).
	 * OAuth2 verwendet CustomOAuth2UserService (für GITEA-Benutzer).
	 */
	@Bean
	@Order(2)
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(auth -> auth
				.requestMatchers("/css/**", "/js/**").permitAll() // Statische Ressourcen
				.requestMatchers("/h2-console/**").permitAll() // H2 Console
				.requestMatchers("/admin/**").hasRole("ADMIN") // Admin-Bereich nur für ADMIN-Rolle
				.anyRequest().authenticated()) // Alle anderen Seiten erfordern Authentifizierung
				.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
				.headers(headers -> headers
						.contentSecurityPolicy(csp -> getPolicyDirectives(csp))
						.xssProtection(Customizer.withDefaults())
						.referrerPolicy(ref -> ref.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
						.frameOptions(frame -> frame.deny()) // Frames grundsätzlich verbieten (außer H2 Console)
						.httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).preload(true))
						.contentTypeOptions(Customizer.withDefaults()))
				.formLogin(form -> form
						.loginPage("/login")
						.permitAll()
						.defaultSuccessUrl("/", true))
				.oauth2Login(oauth2 -> oauth2
						.loginPage("/login")
						.userInfoEndpoint(userInfo -> userInfo
								.userService(customOAuth2UserService))
						.defaultSuccessUrl("/", true)
						.failureUrl("/login?error=true"))
				.logout(logout -> logout
						.logoutSuccessUrl("/login?logout")
						.permitAll());

		return http.build();
	}

	private HeadersConfigurer<HttpSecurity>.ContentSecurityPolicyConfig getPolicyDirectives(
			HeadersConfigurer<HttpSecurity>.ContentSecurityPolicyConfig csp) {
		return csp.policyDirectives(
			"default-src 'none'; " +
			"img-src 'self' data:; " +
			"style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; " +
			"font-src 'self' https://cdn.jsdelivr.net; " +
			"script-src 'self'; " +
			"connect-src 'self' https://projects.sommerhausen.de; " +
			"form-action 'self'; " +
			"base-uri 'none'; " +
			"object-src 'none'; " +
			"frame-ancestors 'none';");
	}
}
