package name.saak.contactmanager.service;

import name.saak.contactmanager.domain.Contact;
import name.saak.contactmanager.repository.ContactRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock
    private ContactRepository contactRepository;

    @InjectMocks
    private ContactService contactService;

    private Contact testContact;

    @BeforeEach
    void setUp() {
        testContact = new Contact("Max", "Mustermann", "Hauptstraße 1", "12345", "Berlin");
        testContact.setId(1L);
        testContact.setEmail("max@example.com");
    }

    @Test
    void shouldFindAllContacts() {
        // Given
        when(contactRepository.findAllByOrderByNachnameAscVornameAsc())
            .thenReturn(List.of(testContact));

        // When
        List<Contact> contacts = contactService.findAllContacts();

        // Then
        assertThat(contacts).hasSize(1);
        assertThat(contacts.get(0).getVorname()).isEqualTo("Max");
        verify(contactRepository).findAllByOrderByNachnameAscVornameAsc();
    }

    @Test
    void shouldFindContactById() {
        // Given
        when(contactRepository.findByIdWithActiveHashtags(1L)).thenReturn(Optional.of(testContact));

        // When
        Optional<Contact> found = contactService.findContactById(1L);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getVorname()).isEqualTo("Max");
        verify(contactRepository).findByIdWithActiveHashtags(1L);
    }

    @Test
    void shouldSearchContactsWithTerm() {
        // Given
        when(contactRepository.searchContacts("Max"))
            .thenReturn(List.of(testContact));

        // When
        List<Contact> results = contactService.searchContacts("Max");

        // Then
        assertThat(results).hasSize(1);
        verify(contactRepository).searchContacts("Max");
    }

    @Test
    void shouldReturnAllContactsWhenSearchTermIsEmpty() {
        // Given
        when(contactRepository.findAllByOrderByNachnameAscVornameAsc())
            .thenReturn(List.of(testContact));

        // When
        List<Contact> results = contactService.searchContacts("");

        // Then
        assertThat(results).hasSize(1);
        verify(contactRepository).findAllByOrderByNachnameAscVornameAsc();
        verify(contactRepository, never()).searchContacts(anyString());
    }

    @Test
    void shouldCreateContact() {
        // Given
        Contact newContact = new Contact("Anna", "Schmidt", "Nebenstraße 5", "54321", "München");
        when(contactRepository.findByNameAndAddress(
            "Anna", "Schmidt", "Nebenstraße 5", "54321", "München"
        )).thenReturn(Optional.empty());
        when(contactRepository.save(any(Contact.class))).thenReturn(newContact);

        // When
        Contact created = contactService.createContact(newContact);

        // Then
        assertThat(created).isNotNull();
        verify(contactRepository).findByNameAndAddress(
            "Anna", "Schmidt", "Nebenstraße 5", "54321", "München"
        );
        verify(contactRepository).save(newContact);
    }

    @Test
    void shouldThrowExceptionWhenCreatingDuplicateContact() {
        // Given
        when(contactRepository.findByNameAndAddress(
            "Max", "Mustermann", "Hauptstraße 1", "12345", "Berlin"
        )).thenReturn(Optional.of(testContact));

        // When/Then
        assertThatThrownBy(() -> contactService.createContact(testContact))
            .isInstanceOf(ContactService.DuplicateContactException.class)
            .hasMessageContaining("existiert bereits");

        verify(contactRepository, never()).save(any());
    }

    @Test
    void shouldUpdateContact() {
        // Given
        Contact updatedData = new Contact("Max", "Mustermann-Neu", "Hauptstraße 2", "12345", "Berlin");
        updatedData.setEmail("new@example.com");

        when(contactRepository.findById(1L)).thenReturn(Optional.of(testContact));
        when(contactRepository.findByNameAndAddress(
            "Max", "Mustermann-Neu", "Hauptstraße 2", "12345", "Berlin"
        )).thenReturn(Optional.empty());
        when(contactRepository.save(any(Contact.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        Contact updated = contactService.updateContact(1L, updatedData);

        // Then
        assertThat(updated.getNachname()).isEqualTo("Mustermann-Neu");
        assertThat(updated.getStrasse()).isEqualTo("Hauptstraße 2");
        assertThat(updated.getEmail()).isEqualTo("new@example.com");
        verify(contactRepository).save(testContact);
    }

    @Test
    void shouldAllowUpdatingContactWithSameNameAddress() {
        // Given
        Contact updatedData = new Contact("Max", "Mustermann", "Hauptstraße 1", "12345", "Berlin");
        updatedData.setEmail("updated@example.com");

        when(contactRepository.findById(1L)).thenReturn(Optional.of(testContact));
        when(contactRepository.findByNameAndAddress(
            "Max", "Mustermann", "Hauptstraße 1", "12345", "Berlin"
        )).thenReturn(Optional.of(testContact)); // Same contact
        when(contactRepository.save(any(Contact.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        Contact updated = contactService.updateContact(1L, updatedData);

        // Then
        assertThat(updated.getEmail()).isEqualTo("updated@example.com");
        verify(contactRepository).save(testContact);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingToExistingContact() {
        // Given
        Contact otherContact = new Contact("Anna", "Schmidt", "Andere Str", "99999", "Hamburg");
        otherContact.setId(2L);

        when(contactRepository.findById(1L)).thenReturn(Optional.of(testContact));
        when(contactRepository.findByNameAndAddress(
            "Anna", "Schmidt", "Andere Str", "99999", "Hamburg"
        )).thenReturn(Optional.of(otherContact));

        Contact updatedData = new Contact("Anna", "Schmidt", "Andere Str", "99999", "Hamburg");

        // When/Then
        assertThatThrownBy(() -> contactService.updateContact(1L, updatedData))
            .isInstanceOf(ContactService.DuplicateContactException.class);

        verify(contactRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentContact() {
        // Given
        when(contactRepository.findById(999L)).thenReturn(Optional.empty());
        Contact updatedData = new Contact("Test", "Test", "Test", "12345", "Test");

        // When/Then
        assertThatThrownBy(() -> contactService.updateContact(999L, updatedData))
            .isInstanceOf(ContactService.ContactNotFoundException.class)
            .hasMessageContaining("nicht gefunden");
    }

    @Test
    void shouldDeleteContact() {
        // Given
        when(contactRepository.existsById(1L)).thenReturn(true);

        // When
        contactService.deleteContact(1L);

        // Then
        verify(contactRepository).existsById(1L);
        verify(contactRepository).deleteById(1L);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentContact() {
        // Given
        when(contactRepository.existsById(999L)).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> contactService.deleteContact(999L))
            .isInstanceOf(ContactService.ContactNotFoundException.class)
            .hasMessageContaining("nicht gefunden");

        verify(contactRepository, never()).deleteById(anyLong());
    }
}
