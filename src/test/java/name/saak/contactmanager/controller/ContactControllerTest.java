package name.saak.contactmanager.controller;

import name.saak.contactmanager.domain.Contact;
import name.saak.contactmanager.service.ContactService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ContactControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContactService contactService;

    @Test
    @WithMockUser
    void shouldDisplayContactListPage() throws Exception {
        mockMvc.perform(get("/contacts"))
            .andExpect(status().isOk())
            .andExpect(view().name("contacts/list"))
            .andExpect(model().attributeExists("contacts"));
    }

    @Test
    @WithMockUser
    void shouldDisplaySearchResults() throws Exception {
        // Given: Test data from Liquibase

        // When/Then
        mockMvc.perform(get("/contacts")
                .param("search", "Mustermann"))
            .andExpect(status().isOk())
            .andExpect(view().name("contacts/list"))
            .andExpect(model().attributeExists("contacts", "searchTerm"))
            .andExpect(model().attribute("searchTerm", "Mustermann"));
    }

    @Test
    @WithMockUser
    void shouldDisplayCreateContactForm() throws Exception {
        mockMvc.perform(get("/contacts/new"))
            .andExpect(status().isOk())
            .andExpect(view().name("contacts/form"))
            .andExpect(model().attributeExists("contact"))
            .andExpect(model().attribute("isEdit", false));
    }

    @Test
    @WithMockUser
    void shouldCreateNewContact() throws Exception {
        mockMvc.perform(post("/contacts")
                .with(csrf())
                .param("vorname", "Test")
                .param("nachname", "Testmann")
                .param("strasse", "Teststraße 1")
                .param("postleitzahl", "11111")
                .param("ort", "Teststadt"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/contacts"))
            .andExpect(flash().attributeExists("successMessage"));

        // Verify contact was created
        var contacts = contactService.searchContacts("Testmann");
        assertThat(contacts).hasSize(1);
        assertThat(contacts.get(0).getVorname()).isEqualTo("Test");
    }

    @Test
    @WithMockUser
    void shouldNotCreateContactWithMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/contacts")
                .with(csrf())
                .param("vorname", "Test")
                .param("nachname", "")) // Missing required field
            .andExpect(status().isOk())
            .andExpect(view().name("contacts/form"))
            .andExpect(model().attributeHasFieldErrors("contact", "nachname"));
    }

    @Test
    @WithMockUser
    void shouldNotCreateDuplicateContact() throws Exception {
        // Given: Create first contact
        contactService.createContact(new Contact("Duplicate", "Test", "Str 1", "12345", "City"));

        // When/Then: Try to create duplicate
        mockMvc.perform(post("/contacts")
                .with(csrf())
                .param("vorname", "Duplicate")
                .param("nachname", "Test")
                .param("strasse", "Str 1")
                .param("postleitzahl", "12345")
                .param("ort", "City"))
            .andExpect(status().isOk())
            .andExpect(view().name("contacts/form"))
            .andExpect(model().attributeExists("errorMessage"))
            .andExpect(model().attribute("errorMessage",
                containsString("existiert bereits")));
    }

    @Test
    @WithMockUser
    void shouldDisplayEditContactForm() throws Exception {
        // Given: Create contact
        Contact contact = contactService.createContact(
            new Contact("Edit", "Test", "Str 1", "12345", "City")
        );

        // When/Then
        mockMvc.perform(get("/contacts/{id}/edit", contact.getId()))
            .andExpect(status().isOk())
            .andExpect(view().name("contacts/form"))
            .andExpect(model().attributeExists("contact"))
            .andExpect(model().attribute("isEdit", true))
            .andExpect(model().attribute("contact", hasProperty("vorname", is("Edit"))));
    }

    @Test
    @WithMockUser
    void shouldUpdateContact() throws Exception {
        // Given: Create contact
        Contact contact = contactService.createContact(
            new Contact("Update", "Original", "Str 1", "12345", "City")
        );

        // When/Then
        mockMvc.perform(post("/contacts/{id}", contact.getId())
                .with(csrf())
                .param("vorname", "Update")
                .param("nachname", "Modified")
                .param("strasse", "Str 1")
                .param("postleitzahl", "12345")
                .param("ort", "City")
                .param("email", "new@example.com"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/contacts"))
            .andExpect(flash().attributeExists("successMessage"));

        // Verify update
        Contact updated = contactService.findContactById(contact.getId()).orElseThrow();
        assertThat(updated.getNachname()).isEqualTo("Modified");
        assertThat(updated.getEmail()).isEqualTo("new@example.com");
    }

    @Test
    @WithMockUser
    void shouldNotUpdateContactWithValidationErrors() throws Exception {
        // Given: Create contact
        Contact contact = contactService.createContact(
            new Contact("Valid", "Contact", "Str 1", "12345", "City")
        );

        // When/Then
        mockMvc.perform(post("/contacts/{id}", contact.getId())
                .with(csrf())
                .param("vorname", "Valid")
                .param("nachname", "") // Invalid: empty
                .param("strasse", "Str 1")
                .param("postleitzahl", "12345")
                .param("ort", "City"))
            .andExpect(status().isOk())
            .andExpect(view().name("contacts/form"))
            .andExpect(model().attributeHasFieldErrors("contact", "nachname"));
    }

    @Test
    @WithMockUser
    void shouldDeleteContact() throws Exception {
        // Given: Create contact
        Contact contact = contactService.createContact(
            new Contact("Delete", "Me", "Str 1", "12345", "City")
        );

        // When/Then
        mockMvc.perform(post("/contacts/{id}/delete", contact.getId())
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/contacts"))
            .andExpect(flash().attributeExists("successMessage"));

        // Verify deletion
        assertThat(contactService.findContactById(contact.getId())).isEmpty();
    }

    @Test
    @WithMockUser
    void shouldHandleDeleteNonExistentContact() throws Exception {
        mockMvc.perform(post("/contacts/{id}/delete", 99999L)
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/contacts"))
            .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    void shouldRequireAuthenticationForContactPages() throws Exception {
        mockMvc.perform(get("/contacts"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser
    void shouldValidateEmailFormat() throws Exception {
        mockMvc.perform(post("/contacts")
                .with(csrf())
                .param("vorname", "Test")
                .param("nachname", "Email")
                .param("strasse", "Str 1")
                .param("postleitzahl", "12345")
                .param("ort", "City")
                .param("email", "invalid-email")) // Invalid format
            .andExpect(status().isOk())
            .andExpect(view().name("contacts/form"))
            .andExpect(model().attributeHasFieldErrors("contact", "email"));
    }
}
