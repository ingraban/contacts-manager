package name.saak.contactmanager.service;

import name.saak.contactmanager.domain.Hashtag;
import name.saak.contactmanager.repository.HashtagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class HashtagService {

    private final HashtagRepository hashtagRepository;

    public HashtagService(HashtagRepository hashtagRepository) {
        this.hashtagRepository = hashtagRepository;
    }

    /**
     * Gibt alle Hashtags sortiert zurück.
     */
    @Transactional(readOnly = true)
    public List<Hashtag> findAllHashtags() {
        return hashtagRepository.findAllByOrderByNameAsc();
    }

    /**
     * Gibt nur aktive (nicht gesperrte) Hashtags sortiert zurück.
     */
    @Transactional(readOnly = true)
    public List<Hashtag> findActiveHashtags() {
        return hashtagRepository.findByGesperrtFalseOrderByNameAsc();
    }

    /**
     * Sucht ein Hashtag anhand der ID.
     */
    @Transactional(readOnly = true)
    public Optional<Hashtag> findHashtagById(Long id) {
        return hashtagRepository.findById(id);
    }

    /**
     * Sucht Hashtags anhand eines Suchbegriffs.
     * Gibt alle Hashtags zurück wenn der Suchbegriff leer ist.
     */
    @Transactional(readOnly = true)
    public List<Hashtag> searchHashtags(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return findAllHashtags();
        }
        return hashtagRepository.searchHashtags(searchTerm.trim());
    }

    /**
     * Speichert ein neues Hashtag.
     *
     * @throws DuplicateHashtagException wenn ein Hashtag mit gleichem Namen existiert
     */
    public Hashtag createHashtag(Hashtag hashtag) {
        validateUniqueHashtagName(hashtag.getName(), null);
        normalizeEmptyFields(hashtag);
        return hashtagRepository.save(hashtag);
    }

    /**
     * Aktualisiert ein bestehendes Hashtag.
     *
     * @throws HashtagNotFoundException wenn das Hashtag nicht gefunden wird
     * @throws DuplicateHashtagException wenn ein Hashtag mit gleichem Namen existiert
     */
    public Hashtag updateHashtag(Long id, Hashtag updatedHashtag) {
        Hashtag existing = hashtagRepository.findById(id)
            .orElseThrow(() -> new HashtagNotFoundException("Hashtag mit ID " + id + " nicht gefunden"));

        validateUniqueHashtagName(updatedHashtag.getName(), id);
        normalizeEmptyFields(updatedHashtag);

        // Update fields (gesperrt wird nicht hier aktualisiert, sondern über lock/unlock)
        existing.setName(updatedHashtag.getName());
        existing.setBeschreibung(updatedHashtag.getBeschreibung());

        return hashtagRepository.save(existing);
    }

    /**
     * Sperrt ein Hashtag (Soft Delete).
     *
     * @throws HashtagNotFoundException wenn das Hashtag nicht gefunden wird
     */
    public void lockHashtag(Long id) {
        Hashtag hashtag = hashtagRepository.findById(id)
            .orElseThrow(() -> new HashtagNotFoundException("Hashtag mit ID " + id + " nicht gefunden"));

        hashtag.setGesperrt(true);
        hashtagRepository.save(hashtag);
    }

    /**
     * Entsperrt ein Hashtag.
     *
     * @throws HashtagNotFoundException wenn das Hashtag nicht gefunden wird
     */
    public void unlockHashtag(Long id) {
        Hashtag hashtag = hashtagRepository.findById(id)
            .orElseThrow(() -> new HashtagNotFoundException("Hashtag mit ID " + id + " nicht gefunden"));

        hashtag.setGesperrt(false);
        hashtagRepository.save(hashtag);
    }

    /**
     * Normalisiert leere Felder zu null.
     */
    private void normalizeEmptyFields(Hashtag hashtag) {
        if (hashtag.getBeschreibung() != null && hashtag.getBeschreibung().trim().isEmpty()) {
            hashtag.setBeschreibung(null);
        }
    }

    /**
     * Validiert dass der Hashtag-Name eindeutig ist.
     *
     * @param name Name des Hashtags
     * @param excludeId ID die ausgeschlossen werden soll (bei Updates)
     * @throws DuplicateHashtagException wenn ein Hashtag mit diesem Namen existiert
     */
    private void validateUniqueHashtagName(String name, Long excludeId) {
        if (name == null || name.trim().isEmpty()) {
            return;
        }

        // Bei Updates: existierendes Hashtag ausschließen
        if (excludeId != null) {
            if (hashtagRepository.existsByNameIgnoreCaseAndIdNot(name, excludeId)) {
                throw new DuplicateHashtagException(
                    "Ein Hashtag mit dem Namen '" + name + "' existiert bereits"
                );
            }
        } else {
            // Bei neuen Hashtags: prüfen ob Name schon existiert
            Optional<Hashtag> duplicate = hashtagRepository.findByNameIgnoreCase(name);
            if (duplicate.isPresent()) {
                throw new DuplicateHashtagException(
                    "Ein Hashtag mit dem Namen '" + name + "' existiert bereits"
                );
            }
        }
    }

    /**
     * Exception für nicht gefundene Hashtags.
     */
    public static class HashtagNotFoundException extends RuntimeException {
        private static final long serialVersionUID = 202512261430L;

        public HashtagNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Exception für doppelte Hashtags.
     */
    public static class DuplicateHashtagException extends RuntimeException {
        private static final long serialVersionUID = 202512261431L;

        public DuplicateHashtagException(String message) {
            super(message);
        }
    }
}
