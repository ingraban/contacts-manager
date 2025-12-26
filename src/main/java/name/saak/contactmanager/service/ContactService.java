package name.saak.contactmanager.service;

import name.saak.contactmanager.domain.Contact;
import name.saak.contactmanager.repository.ContactRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ContactService {

    private final ContactRepository contactRepository;

    public ContactService(ContactRepository contactRepository) {
        this.contactRepository = contactRepository;
    }

    /**
     * Gibt alle Kontakte sortiert zurück.
     */
    @Transactional(readOnly = true)
    public List<Contact> findAllContacts() {
        return contactRepository.findAllByOrderByNachnameAscVornameAsc();
    }

    /**
     * Sucht einen Kontakt anhand der ID.
     */
    @Transactional(readOnly = true)
    public Optional<Contact> findContactById(Long id) {
        return contactRepository.findById(id);
    }

    /**
     * Sucht Kontakte mit Volltextsuche.
     */
    @Transactional(readOnly = true)
    public List<Contact> searchContacts(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return findAllContacts();
        }
        return contactRepository.searchContacts(searchTerm.trim());
    }

    /**
     * Speichert einen neuen Kontakt.
     *
     * @throws DuplicateContactException wenn ein Kontakt mit gleicher Name-Adresse-Kombination existiert
     */
    public Contact createContact(Contact contact) {
        validateUniqueConstraint(contact, null);
        normalizeEmptyFields(contact);
        return contactRepository.save(contact);
    }

    /**
     * Aktualisiert einen bestehenden Kontakt.
     *
     * @throws DuplicateContactException wenn ein Kontakt mit gleicher Name-Adresse-Kombination existiert
     */
    public Contact updateContact(Long id, Contact updatedContact) {
        Contact existing = contactRepository.findById(id)
            .orElseThrow(() -> new ContactNotFoundException("Kontakt mit ID " + id + " nicht gefunden"));

        validateUniqueConstraint(updatedContact, id);
        normalizeEmptyFields(updatedContact);

        // Update fields
        existing.setAnrede(updatedContact.getAnrede());
        existing.setVorname(updatedContact.getVorname());
        existing.setNachname(updatedContact.getNachname());
        existing.setStrasse(updatedContact.getStrasse());
        existing.setPostleitzahl(updatedContact.getPostleitzahl());
        existing.setOrt(updatedContact.getOrt());
        existing.setTelefon1(updatedContact.getTelefon1());
        existing.setTelefon2(updatedContact.getTelefon2());
        existing.setEmail(updatedContact.getEmail());

        return contactRepository.save(existing);
    }

    /**
     * Löscht einen Kontakt.
     */
    public void deleteContact(Long id) {
        if (!contactRepository.existsById(id)) {
            throw new ContactNotFoundException("Kontakt mit ID " + id + " nicht gefunden");
        }
        contactRepository.deleteById(id);
    }

    /**
     * Normalisiert leere Felder zu null.
     * Konvertiert leere Strings für Telefon und E-Mail zu null.
     */
    private void normalizeEmptyFields(Contact contact) {
        if (contact.getTelefon1() != null && contact.getTelefon1().trim().isEmpty()) {
            contact.setTelefon1(null);
        }
        if (contact.getTelefon2() != null && contact.getTelefon2().trim().isEmpty()) {
            contact.setTelefon2(null);
        }
        if (contact.getEmail() != null && contact.getEmail().trim().isEmpty()) {
            contact.setEmail(null);
        }
    }

    /**
     * Validiert die Unique-Constraint-Regel.
     */
    private void validateUniqueConstraint(Contact contact, Long excludeId) {
        Optional<Contact> duplicate = contactRepository.findByNameAndAddress(
            contact.getVorname(),
            contact.getNachname(),
            contact.getStrasse(),
            contact.getPostleitzahl(),
            contact.getOrt()
        );

        if (duplicate.isPresent() && !duplicate.get().getId().equals(excludeId)) {
            throw new DuplicateContactException(
                "Ein Kontakt mit diesem Namen und dieser Adresse existiert bereits"
            );
        }
    }

    /**
     * Exception für nicht gefundene Kontakte.
     */
    public static class ContactNotFoundException extends RuntimeException {
        private static final long serialVersionUID = 202512261415L;

		public ContactNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Exception für doppelte Kontakte.
     */
    public static class DuplicateContactException extends RuntimeException {
        private static final long serialVersionUID = 202512261416L;

		public DuplicateContactException(String message) {
            super(message);
        }
    }
}
