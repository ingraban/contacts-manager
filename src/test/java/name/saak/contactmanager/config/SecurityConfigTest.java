package name.saak.contactmanager.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
	void protectedPagesRedirectToLoginWhenNotAuthenticated() throws Exception {
		mockMvc.perform(get("/contacts"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrlPattern("**/login"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void adminCanAccessAdminPages() throws Exception {
		mockMvc.perform(get("/admin/users"))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(roles = "USER")
	void userCannotAccessAdminPages() throws Exception {
		mockMvc.perform(get("/admin/users"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "CONTACT")
	void contactCanAccessContacts() throws Exception {
		mockMvc.perform(get("/contacts"))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(roles = "CONTACT")
	void contactCannotAccessAdminPages() throws Exception {
		mockMvc.perform(get("/admin/users"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser
	void securityHeadersAreSet() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(header().exists("Content-Security-Policy"))
				.andExpect(header().string("Content-Security-Policy",
					"default-src 'none'; img-src 'self' data:; style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; font-src 'self' https://cdn.jsdelivr.net; script-src 'self'; connect-src 'self' https://projects.sommerhausen.de; form-action 'self'; base-uri 'none'; object-src 'none'; frame-ancestors 'none';"))
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
	void userDetailsServiceContainsContactUser() {
		var userDetails = userDetailsService.loadUserByUsername("contact");

		assertThat(userDetails).isNotNull();
		assertThat(userDetails.getUsername()).isEqualTo("contact");
		assertThat(userDetails.getAuthorities()).hasSize(1);
		assertThat(userDetails.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_CONTACT");
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void csrfProtectionIsEnabled() throws Exception {
		// CSRF-Schutz ist aktiviert, wenn POST ohne Token zu 403 führt
		mockMvc.perform(post("/admin/database/backup")
						.contentType("application/x-www-form-urlencoded"))
				.andExpect(status().isForbidden());
	}
}
