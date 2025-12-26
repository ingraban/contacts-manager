package name.saak.contactmanager.repository;

import name.saak.contactmanager.domain.Hashtag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HashtagRepository extends JpaRepository<Hashtag, Long> {

    /**
     * Findet ein Hashtag anhand seines Namens (case-insensitive).
     */
    Optional<Hashtag> findByNameIgnoreCase(String name);

    /**
     * Findet alle aktiven (nicht gesperrten) Hashtags sortiert nach Name.
     */
    List<Hashtag> findByGesperrtFalseOrderByNameAsc();

    /**
     * Findet alle Hashtags sortiert nach Name.
     */
    List<Hashtag> findAllByOrderByNameAsc();

    /**
     * Sucht Hashtags anhand eines Suchbegriffs.
     * Durchsucht Name und Beschreibung mit SQL LIKE (case-insensitive).
     */
    @Query("SELECT h FROM Hashtag h WHERE " +
           "LOWER(h.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(COALESCE(h.beschreibung, '')) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY h.name ASC")
    List<Hashtag> searchHashtags(@Param("searchTerm") String searchTerm);

    /**
     * Prüft ob ein Hashtag mit dem gleichen Namen existiert (außer der angegebenen ID).
     * Wird für Unique-Constraint-Validierung beim Update verwendet.
     */
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
