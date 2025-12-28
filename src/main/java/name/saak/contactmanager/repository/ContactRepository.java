package name.saak.contactmanager.repository;

import name.saak.contactmanager.domain.Contact;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {

    /**
     * Sucht Kontakte anhand eines Suchbegriffs mit eager loading der Hashtags.
     * Durchsucht alle Felder mit SQL LIKE (case-insensitive).
     * WICHTIG: Lädt ALLE Hashtags (auch gesperrte), Filterung erfolgt in der View.
     */
    @Query("SELECT DISTINCT c FROM Contact c " +
           "LEFT JOIN FETCH c.hashtags " +
           "WHERE " +
           "LOWER(c.vorname) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.nachname) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.strasse) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.postleitzahl) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.ort) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(COALESCE(c.anrede, '')) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(COALESCE(c.telefon1, '')) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(COALESCE(c.telefon2, '')) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(COALESCE(c.email, '')) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(COALESCE(c.firma, '')) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(COALESCE(c.bemerkung, '')) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Contact> searchContacts(@Param("searchTerm") String searchTerm);

    /**
     * Findet alle Kontakte sortiert nach Nachname, dann Vorname mit eager loading der Hashtags.
     * WICHTIG: Lädt ALLE Hashtags (auch gesperrte), Filterung erfolgt in der View.
     */
    @Query("SELECT DISTINCT c FROM Contact c " +
           "LEFT JOIN FETCH c.hashtags " +
           "ORDER BY c.nachname ASC, c.vorname ASC")
    List<Contact> findAllByOrderByNachnameAscVornameAsc();

    /**
     * Prüft ob ein Kontakt mit der gleichen Name-Adresse-Kombination existiert.
     * Wird für Unique-Constraint-Validierung vor dem Speichern verwendet.
     */
    @Query("SELECT c FROM Contact c WHERE " +
           "LOWER(c.vorname) = LOWER(:vorname) AND " +
           "LOWER(c.nachname) = LOWER(:nachname) AND " +
           "LOWER(c.strasse) = LOWER(:strasse) AND " +
           "LOWER(c.postleitzahl) = LOWER(:postleitzahl) AND " +
           "LOWER(c.ort) = LOWER(:ort)")
    Optional<Contact> findByNameAndAddress(
        @Param("vorname") String vorname,
        @Param("nachname") String nachname,
        @Param("strasse") String strasse,
        @Param("postleitzahl") String postleitzahl,
        @Param("ort") String ort
    );

    /**
     * Findet Kontakte nach Ort.
     */
    List<Contact> findByOrtIgnoreCaseOrderByNachnameAsc(String ort);

    /**
     * Findet Kontakte die ALLE angegebenen Hashtags haben (AND-Logik) mit eager loading.
     * Nur aktive (nicht gesperrte) Hashtags werden berücksichtigt.
     */
    @Query("SELECT DISTINCT c FROM Contact c " +
           "LEFT JOIN FETCH c.hashtags " +
           "WHERE c.id IN (" +
           "  SELECT c2.id FROM Contact c2 " +
           "  JOIN c2.hashtags h " +
           "  WHERE LOWER(h.name) IN :hashtagNames " +
           "  AND h.gesperrt = false " +
           "  GROUP BY c2.id " +
           "  HAVING COUNT(DISTINCT h.name) = :count" +
           ") " +
           "ORDER BY c.nachname ASC, c.vorname ASC")
    List<Contact> findByAllHashtags(
        @Param("hashtagNames") List<String> hashtagNames,
        @Param("count") long count
    );

    /**
     * Findet einen Kontakt mit eager loading aller Hashtags.
     * Verhindert N+1 Probleme beim Laden der Hashtags.
     */
    @Query("SELECT DISTINCT c FROM Contact c " +
           "LEFT JOIN FETCH c.hashtags " +
           "WHERE c.id = :id")
    Optional<Contact> findByIdWithActiveHashtags(@Param("id") Long id);

    /**
     * Findet alle Kontakt-IDs mit dynamischer Sortierung (ohne JOIN für korrekte Sortierung).
     */
    @Query("SELECT c.id FROM Contact c")
    List<Long> findAllContactIds(Sort sort);

    /**
     * Findet Kontakte anhand von IDs mit eager loading.
     * WICHTIG: Lädt ALLE Hashtags (auch gesperrte), Filterung erfolgt in der View.
     */
    @Query("SELECT DISTINCT c FROM Contact c " +
           "LEFT JOIN FETCH c.hashtags " +
           "WHERE c.id IN :ids")
    List<Contact> findByIdsWithHashtags(@Param("ids") List<Long> ids);

    /**
     * Sucht Kontakt-IDs mit dynamischer Sortierung (ohne JOIN für korrekte Sortierung).
     */
    @Query("SELECT c.id FROM Contact c " +
           "WHERE " +
           "LOWER(c.vorname) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.nachname) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.strasse) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.postleitzahl) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.ort) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(COALESCE(c.anrede, '')) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(COALESCE(c.telefon1, '')) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(COALESCE(c.telefon2, '')) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(COALESCE(c.email, '')) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(COALESCE(c.firma, '')) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(COALESCE(c.bemerkung, '')) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Long> searchContactIds(@Param("searchTerm") String searchTerm, Sort sort);
}
