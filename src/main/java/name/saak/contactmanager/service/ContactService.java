package name.saak.contactmanager.service;

import name.saak.contactmanager.domain.Contact;
import name.saak.contactmanager.domain.Hashtag;
import name.saak.contactmanager.repository.ContactRepository;
import name.saak.contactmanager.repository.HashtagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class ContactService {

    private final ContactRepository contactRepository;
    private final HashtagRepository hashtagRepository;

    public ContactService(ContactRepository contactRepository, HashtagRepository hashtagRepository) {
        this.contactRepository = contactRepository;
        this.hashtagRepository = hashtagRepository;
    }

    /**
     * Gibt alle Kontakte sortiert zurück.
     */
    @Transactional(readOnly = true)
    public List<Contact> findAllContacts() {
        return contactRepository.findAllByOrderByNachnameAscVornameAsc();
    }

    /**
     * Sucht einen Kontakt anhand der ID mit eager loading der Hashtags.
     */
    @Transactional(readOnly = true)
    public Optional<Contact> findContactById(Long id) {
        return contactRepository.findByIdWithActiveHashtags(id);
    }

    /**
     * Sucht Kontakte mit Volltextsuche oder Hashtag-Suche.
     * Wenn der Suchbegriff mit # beginnt, wird nach Hashtags gesucht.
     */
    @Transactional(readOnly = true)
    public List<Contact> searchContacts(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return findAllContacts();
        }

        String trimmedSearchTerm = searchTerm.trim();

        // Hashtag-Suche wenn Begriff mit # beginnt
        if (trimmedSearchTerm.startsWith("#")) {
            return searchByHashtags(trimmedSearchTerm);
        }

        // Normale Volltextsuche
        return contactRepository.searchContacts(trimmedSearchTerm);
    }

    /**
     * Sucht Kontakte anhand von Hashtags (AND-Logik).
     * Mehrere Hashtags können durch Leerzeichen getrennt werden.
     */
    @Transactional(readOnly = true)
    public List<Contact> searchByHashtags(String searchTerm) {
        // Parse hashtag names from search term
        List<String> hashtagNames = Arrays.stream(searchTerm.split("\\s+"))
            .filter(term -> term.startsWith("#"))
            .map(String::toLowerCase)
            .collect(Collectors.toList());

        if (hashtagNames.isEmpty()) {
            return List.of();
        }

        // Use AND logic: contact must have ALL hashtags
        return contactRepository.findByAllHashtags(hashtagNames, hashtagNames.size());
    }

    /**
     * Speichert einen neuen Kontakt.
     *
     * @throws DuplicateContactException wenn ein Kontakt mit gleicher Name-Adresse-Kombination existiert
     */
    public Contact createContact(Contact contact) {
        return createContact(contact, null);
    }

    /**
     * Speichert einen neuen Kontakt mit Hashtags.
     *
     * @param contact Kontakt-Objekt
     * @param hashtagIds Set von Hashtag-IDs (optional)
     * @throws DuplicateContactException wenn ein Kontakt mit gleicher Name-Adresse-Kombination existiert
     */
    public Contact createContact(Contact contact, Set<Long> hashtagIds) {
        validateUniqueConstraint(contact, null);
        normalizeEmptyFields(contact);

        // Hashtags zuordnen (nur aktive)
        if (hashtagIds != null && !hashtagIds.isEmpty()) {
            Set<Hashtag> hashtags = hashtagIds.stream()
                .map(hashtagRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(h -> !h.isGesperrt())
                .collect(Collectors.toSet());
            contact.setHashtags(hashtags);
        }

        return contactRepository.save(contact);
    }

    /**
     * Aktualisiert einen bestehenden Kontakt.
     *
     * @throws DuplicateContactException wenn ein Kontakt mit gleicher Name-Adresse-Kombination existiert
     */
    public Contact updateContact(Long id, Contact updatedContact) {
        return updateContact(id, updatedContact, null);
    }

    /**
     * Aktualisiert einen bestehenden Kontakt mit Hashtags.
     *
     * @param id ID des Kontakts
     * @param updatedContact Aktualisierte Kontaktdaten
     * @param hashtagIds Set von Hashtag-IDs (optional, null = keine Änderung)
     * @throws DuplicateContactException wenn ein Kontakt mit gleicher Name-Adresse-Kombination existiert
     */
    public Contact updateContact(Long id, Contact updatedContact, Set<Long> hashtagIds) {
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
        existing.setFirma(updatedContact.getFirma());
        existing.setBemerkung(updatedContact.getBemerkung());

        // Update hashtags if provided
        if (hashtagIds != null) {
            existing.getHashtags().clear();
            Set<Hashtag> hashtags = hashtagIds.stream()
                .map(hashtagRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(h -> !h.isGesperrt())
                .collect(Collectors.toSet());
            existing.setHashtags(hashtags);
        }

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
     * Konvertiert leere Strings für optionale Felder (Telefon, E-Mail, Firma, Bemerkung) zu null.
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
        if (contact.getFirma() != null && contact.getFirma().trim().isEmpty()) {
            contact.setFirma(null);
        }
        if (contact.getBemerkung() != null && contact.getBemerkung().trim().isEmpty()) {
            contact.setBemerkung(null);
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
