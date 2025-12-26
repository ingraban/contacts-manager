package name.saak.contactmanager.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserDetailsService userDetailsService;

	@Test
	void staticCssResourcesAreAccessibleWithoutAuthentication() throws Exception {
		mockMvc.perform(get("/css/styles.css"))
				.andExpect(status().isOk());
	}

	@Test
	void staticJsResourcesAreAccessibleWithoutAuthentication() throws Exception {
		mockMvc.perform(get("/js/app.js"))
				.andExpect(status().isOk());
	}

	@Test
	void loginPageIsAccessibleWithoutAuthentication() throws Exception {
		mockMvc.perform(get("/login"))
				.andExpect(status().isOk());
	}

	@Test
	void protectedPagesRedirectToLoginWhenNotAuthenticated() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrlPattern("**/login"));
	}

	@Test
	void authenticatedUserCanAccessProtectedPages() throws Exception {
		mockMvc.perform(formLogin("/login")
						.user("admin")
						.password("geheim"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/"))
				.andExpect(authenticated().withUsername("admin"));
	}

	@Test
	void loginWithInvalidCredentialsFails() throws Exception {
		mockMvc.perform(formLogin("/login")
						.user("admin")
						.password("wrong"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login?error"))
				.andExpect(unauthenticated());
	}

	@Test
	@WithMockUser
	void securityHeadersAreSet() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(header().exists("Content-Security-Policy"))
				.andExpect(header().string("Content-Security-Policy",
					"default-src 'none'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self'; form-action 'self'; base-uri 'none'; object-src 'none'; frame-ancestors 'none';"))
				.andExpect(header().exists("X-Content-Type-Options"))
				.andExpect(header().string("X-Content-Type-Options", "nosniff"))
				.andExpect(header().exists("X-Frame-Options"))
				.andExpect(header().string("X-Frame-Options", "DENY"))
				.andExpect(header().exists("Referrer-Policy"))
				.andExpect(header().string("Referrer-Policy", "no-referrer"));
	}

	@Test
	@WithMockUser
	void hstsHeaderIsSetWhenUsingHttps() throws Exception {
		// HSTS wird nur über HTTPS gesetzt, nicht in HTTP-Tests
		// Hier prüfen wir, dass die SecurityConfig HSTS konfiguriert hat,
		// aber der Header wird nur bei echten HTTPS-Requests gesetzt
		mockMvc.perform(get("/").secure(true))
				.andExpect(status().isOk())
				.andExpect(header().exists("Strict-Transport-Security"))
				.andExpect(header().string("Strict-Transport-Security",
					"max-age=31536000 ; includeSubDomains ; preload"));
	}

	@Test
	void userDetailsServiceContainsAdminUser() {
		var userDetails = userDetailsService.loadUserByUsername("admin");

		assertThat(userDetails).isNotNull();
		assertThat(userDetails.getUsername()).isEqualTo("admin");
		assertThat(userDetails.getAuthorities()).hasSize(1);
		assertThat(userDetails.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_ADMIN");
	}

	@Test
	@WithMockUser
	void csrfProtectionIsEnabled() throws Exception {
		// CSRF-Token sollte in Cookies vorhanden sein (CookieCsrfTokenRepository)
		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(result -> {
					var cookies = result.getResponse().getCookies();
					boolean hasCsrfCookie = false;
					for (var cookie : cookies) {
						if (cookie.getName().equals("XSRF-TOKEN")) {
							hasCsrfCookie = true;
							break;
						}
					}
					assertThat(hasCsrfCookie).isTrue();
				});
	}
}
