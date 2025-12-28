package name.saak.contactmanager.service;

import name.saak.contactmanager.domain.Contact;
import name.saak.contactmanager.domain.Hashtag;
import name.saak.contactmanager.repository.ContactRepository;
import name.saak.contactmanager.repository.HashtagRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
     * Gibt alle Kontakte mit dynamischer Sortierung zurück.
     *
     * @param sortField Sortierfeld (vorname, nachname, firma, adresse) oder null für Default
     * @param sortDir Sortierrichtung (asc, desc) oder null für Default
     */
    @Transactional(readOnly = true)
    public List<Contact> findAllContacts(String sortField, String sortDir) {
        if (sortField == null || sortField.isEmpty()) {
            return findAllContacts();
        }

        Sort sort = createSort(sortField, sortDir);

        // Two-step approach: first get IDs with correct sort, then fetch entities
        List<Long> ids = contactRepository.findAllContactIds(sort);
        if (ids.isEmpty()) {
            return List.of();
        }

        List<Contact> contacts = contactRepository.findByIdsWithHashtags(ids);

        // Maintain sort order (IN clause doesn't preserve order)
        return sortContactsByIds(contacts, ids);
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
     * Sucht Kontakte mit Volltextsuche und dynamischer Sortierung.
     *
     * @param searchTerm Suchbegriff
     * @param sortField Sortierfeld oder null für Default
     * @param sortDir Sortierrichtung oder null für Default
     */
    @Transactional(readOnly = true)
    public List<Contact> searchContacts(String searchTerm, String sortField, String sortDir) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return findAllContacts(sortField, sortDir);
        }

        String trimmedSearchTerm = searchTerm.trim();

        // Hashtag-Suche unterstützt aktuell keine benutzerdefinierte Sortierung
        if (trimmedSearchTerm.startsWith("#")) {
            return searchByHashtags(trimmedSearchTerm);
        }

        // Normale Volltextsuche mit Sortierung
        if (sortField == null || sortField.isEmpty()) {
            return contactRepository.searchContacts(trimmedSearchTerm);
        }

        Sort sort = createSort(sortField, sortDir);

        // Two-step approach: first get IDs with correct sort, then fetch entities
        List<Long> ids = contactRepository.searchContactIds(trimmedSearchTerm, sort);
        if (ids.isEmpty()) {
            return List.of();
        }

        List<Contact> contacts = contactRepository.findByIdsWithHashtags(ids);

        // Maintain sort order (IN clause doesn't preserve order)
        return sortContactsByIds(contacts, ids);
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

        // Hashtags zuordnen (nur aktive) - use helper method for bidirectional relationship
        if (hashtagIds != null && !hashtagIds.isEmpty()) {
            Set<Hashtag> hashtags = hashtagIds.stream()
                .map(hashtagRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(h -> !h.isGesperrt())
                .collect(Collectors.toSet());
            hashtags.forEach(contact::addHashtag);
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

        // Update hashtags - properly manage bidirectional relationship
        // First, remove all existing hashtags using the helper method
        Set<Hashtag> existingHashtags = new HashSet<>(existing.getHashtags());
        existingHashtags.forEach(existing::removeHashtag);

        // Then add the new ones (if any provided)
        if (hashtagIds != null && !hashtagIds.isEmpty()) {
            Set<Hashtag> hashtags = hashtagIds.stream()
                .map(hashtagRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(h -> !h.isGesperrt())
                .collect(Collectors.toSet());
            hashtags.forEach(existing::addHashtag);
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
     * Weist mehreren Kontakten einen Hashtag zu.
     *
     * @param contactIds Liste der Kontakt-IDs
     * @param hashtagId ID des zuzuweisenden Hashtags
     */
    public void assignHashtagToContacts(List<Long> contactIds, Long hashtagId) {
        // Lade Hashtag
        Hashtag hashtag = hashtagRepository.findById(hashtagId)
            .orElseThrow(() -> new IllegalArgumentException("Hashtag mit ID " + hashtagId + " nicht gefunden"));

        // Prüfe ob Hashtag gesperrt ist
        if (hashtag.isGesperrt()) {
            throw new IllegalArgumentException("Gesperrte Hashtags können nicht zugewiesen werden");
        }

        // Lade alle Kontakte und füge Hashtag hinzu
        for (Long contactId : contactIds) {
            Optional<Contact> optionalContact = contactRepository.findById(contactId);
            if (optionalContact.isPresent()) {
                Contact contact = optionalContact.get();
                // Füge Hashtag nur hinzu wenn er noch nicht zugewiesen ist
                if (!contact.getHashtags().contains(hashtag)) {
                    contact.addHashtag(hashtag);
                    contactRepository.save(contact);
                }
            }
        }
    }

    /**
     * Entfernt einen Hashtag von mehreren Kontakten.
     *
     * @param contactIds Liste der Kontakt-IDs
     * @param hashtagId ID des zu entfernenden Hashtags
     */
    public void removeHashtagFromContacts(List<Long> contactIds, Long hashtagId) {
        // Lade Hashtag
        Hashtag hashtag = hashtagRepository.findById(hashtagId)
            .orElseThrow(() -> new IllegalArgumentException("Hashtag mit ID " + hashtagId + " nicht gefunden"));

        // Lade alle Kontakte und entferne Hashtag
        for (Long contactId : contactIds) {
            Optional<Contact> optionalContact = contactRepository.findById(contactId);
            if (optionalContact.isPresent()) {
                Contact contact = optionalContact.get();
                // Entferne Hashtag nur wenn er zugewiesen ist
                if (contact.getHashtags().contains(hashtag)) {
                    contact.removeHashtag(hashtag);
                    contactRepository.save(contact);
                }
            }
        }
    }

    /**
     * Erstellt ein Sort-Objekt basierend auf Feld und Richtung.
     *
     * @param sortField Das Sortierfeld (vorname, nachname, firma, adresse)
     * @param sortDir Die Sortierrichtung (asc, desc)
     * @return Sort-Objekt für die Datenbank-Query
     */
    private Sort createSort(String sortField, String sortDir) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir)
            ? Sort.Direction.DESC
            : Sort.Direction.ASC;

        return switch (sortField.toLowerCase()) {
            case "vorname" -> Sort.by(direction, "vorname", "nachname");
            case "nachname" -> Sort.by(direction, "nachname", "vorname");
            case "firma" -> Sort.by(direction, "firma", "nachname", "vorname");
            case "adresse" -> Sort.by(direction, "ort", "strasse", "nachname", "vorname");
            default -> Sort.by(Sort.Direction.ASC, "nachname", "vorname");
        };
    }

    /**
     * Sortiert eine Liste von Kontakten nach einer gegebenen ID-Reihenfolge.
     * Erforderlich, weil SQL IN clause die Sortierung nicht bewahrt.
     *
     * @param contacts Liste der zu sortierenden Kontakte
     * @param ids Liste der IDs in der gewünschten Reihenfolge
     * @return Sortierte Liste von Kontakten
     */
    private List<Contact> sortContactsByIds(List<Contact> contacts, List<Long> ids) {
        // Create a map for O(1) lookup
        Map<Long, Contact> contactMap = contacts.stream()
            .collect(Collectors.toMap(Contact::getId, c -> c));

        // Return contacts in the order of the IDs
        return ids.stream()
            .map(contactMap::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
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
