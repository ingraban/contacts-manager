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

    @Test
    void shouldSaveAndRetrieveContactWithFirma() {
        // Given
        Contact contact = new Contact();
        contact.setVorname("Anna");
        contact.setNachname("Schmidt");
        contact.setStrasse("Teststraße 5");
        contact.setPostleitzahl("54321");
        contact.setOrt("München");
        contact.setFirma("Test GmbH");

        // When
        Contact saved = contactRepository.save(contact);
        Contact retrieved = contactRepository.findById(saved.getId()).orElseThrow();

        // Then
        assertThat(retrieved.getFirma()).isEqualTo("Test GmbH");
    }

    @Test
    void shouldSaveAndRetrieveContactWithBemerkung() {
        // Given
        Contact contact = new Contact();
        contact.setVorname("Peter");
        contact.setNachname("Weber");
        contact.setStrasse("Teststraße 10");
        contact.setPostleitzahl("98765");
        contact.setOrt("Hamburg");
        contact.setBemerkung("Dies ist eine wichtige Testbemerkung für den Kontakt");

        // When
        Contact saved = contactRepository.save(contact);
        Contact retrieved = contactRepository.findById(saved.getId()).orElseThrow();

        // Then
        assertThat(retrieved.getBemerkung()).isEqualTo("Dies ist eine wichtige Testbemerkung für den Kontakt");
    }

    @Test
    void shouldSearchContactsByFirma() {
        // Given
        Contact contact = new Contact();
        contact.setVorname("Maria");
        contact.setNachname("Müller");
        contact.setStrasse("Firmenstraße 1");
        contact.setPostleitzahl("11111");
        contact.setOrt("Frankfurt");
        contact.setFirma("Beispiel AG");
        contactRepository.save(contact);

        // When
        List<Contact> results = contactRepository.searchContacts("Beispiel AG");

        // Then
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getFirma()).isEqualTo("Beispiel AG");
    }

    @Test
    void shouldSearchContactsByBemerkung() {
        // Given
        Contact contact = new Contact();
        contact.setVorname("Thomas");
        contact.setNachname("Klein");
        contact.setStrasse("Bemerkungsweg 2");
        contact.setPostleitzahl("22222");
        contact.setOrt("Köln");
        contact.setBemerkung("Wichtiger VIP-Kunde");
        contactRepository.save(contact);

        // When
        List<Contact> results = contactRepository.searchContacts("VIP-Kunde");

        // Then
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getBemerkung()).contains("VIP-Kunde");
    }

    @Test
    void shouldUpdateContactWithFirma() {
        // Given - Create contact without firma
        Contact contact = new Contact();
        contact.setVorname("Julia");
        contact.setNachname("Fischer");
        contact.setStrasse("Updatestraße 1");
        contact.setPostleitzahl("33333");
        contact.setOrt("Stuttgart");
        Contact saved = contactRepository.save(contact);

        assertThat(saved.getFirma()).isNull();

        // When - Update contact with firma
        saved.setFirma("Neue Firma GmbH");
        Contact updated = contactRepository.save(saved);

        // Then - Verify firma was updated
        Contact retrieved = contactRepository.findById(updated.getId()).orElseThrow();
        assertThat(retrieved.getFirma()).isEqualTo("Neue Firma GmbH");
        assertThat(retrieved.getVorname()).isEqualTo("Julia");
        assertThat(retrieved.getNachname()).isEqualTo("Fischer");
    }
}
