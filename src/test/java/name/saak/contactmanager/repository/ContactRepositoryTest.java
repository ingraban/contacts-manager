package name.saak.contactmanager.repository;

import name.saak.contactmanager.domain.Contact;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ContactRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ContactRepository contactRepository;

    @Test
    void shouldSaveAndFindContact() {
        // Given
        Contact contact = new Contact("TestMax", "TestMustermann", "Teststraße 99", "99999", "Teststadt");
        contact.setAnrede("Herr");
        contact.setEmail("testmax@example.com");

        // When
        Contact saved = contactRepository.save(contact);
        entityManager.flush();
        Optional<Contact> found = contactRepository.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getVorname()).isEqualTo("TestMax");
        assertThat(found.get().getEmail()).isEqualTo("testmax@example.com");
    }

    @Test
    void shouldFindAllContactsSortedByName() {
        // Given
        contactRepository.save(new Contact("Zara", "Zimmermann", "Str 1", "11111", "Stadt"));
        contactRepository.save(new Contact("Anna", "Abel", "Str 2", "22222", "Stadt"));
        contactRepository.save(new Contact("Max", "Müller", "Str 3", "33333", "Stadt"));
        entityManager.flush();

        // When
        List<Contact> contacts = contactRepository.findAllByOrderByNachnameAscVornameAsc();

        // Then - includes test data from Liquibase
        assertThat(contacts).hasSize(6); // 3 new + 3 from Liquibase
        // Verify first and last are sorted correctly
        assertThat(contacts.get(0).getNachname()).isEqualTo("Abel");
        assertThat(contacts.get(contacts.size() - 1).getNachname()).isEqualTo("Zimmermann");
    }

    @Test
    void shouldSearchContactsByVorname() {
        // Given (using test data from Liquibase)

        // When
        List<Contact> results = contactRepository.searchContacts("Max");

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getVorname()).isEqualTo("Max");
    }

    @Test
    void shouldSearchContactsByOrt() {
        // Given (using test data from Liquibase - Max in Berlin)

        // When
        List<Contact> results = contactRepository.searchContacts("Berlin");

        // Then
        assertThat(results).hasSize(1);
    }

    @Test
    void shouldSearchContactsCaseInsensitive() {
        // Given (using test data from Liquibase)

        // When
        List<Contact> results = contactRepository.searchContacts("MUSTERMANN");

        // Then
        assertThat(results).hasSize(1);
    }

    @Test
    void shouldSearchContactsByEmail() {
        // Given (using test data from Liquibase - max@example.com)

        // When
        List<Contact> results = contactRepository.searchContacts("max@example");

        // Then
        assertThat(results).hasSize(1);
    }

    @Test
    void shouldFindByNameAndAddress() {
        // Given (using test data from Liquibase)

        // When
        Optional<Contact> found = contactRepository.findByNameAndAddress(
            "Max", "Mustermann", "Hauptstraße 1", "12345", "Berlin"
        );

        // Then
        assertThat(found).isPresent();
    }

    @Test
    void shouldFindByNameAndAddressCaseInsensitive() {
        // Given (using test data from Liquibase)

        // When
        Optional<Contact> found = contactRepository.findByNameAndAddress(
            "max", "MUSTERMANN", "hauptstraße 1", "12345", "berlin"
        );

        // Then
        assertThat(found).isPresent();
    }

    @Test
    void shouldFindContactsByOrt() {
        // Given (using test data from Liquibase - Max Mustermann in Berlin)

        // When
        List<Contact> results = contactRepository.findByOrtIgnoreCaseOrderByNachnameAsc("Berlin");

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getNachname()).isEqualTo("Mustermann");
    }
}
