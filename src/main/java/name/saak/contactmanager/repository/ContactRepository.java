package name.saak.contactmanager.repository;

import name.saak.contactmanager.domain.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {

    /**
     * Sucht Kontakte anhand eines Suchbegriffs.
     * Durchsucht alle Felder mit SQL LIKE (case-insensitive).
     */
    @Query("SELECT c FROM Contact c WHERE " +
           "LOWER(c.vorname) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.nachname) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.strasse) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.postleitzahl) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.ort) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(COALESCE(c.anrede, '')) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(COALESCE(c.telefon1, '')) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(COALESCE(c.telefon2, '')) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(COALESCE(c.email, '')) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Contact> searchContacts(@Param("searchTerm") String searchTerm);

    /**
     * Findet alle Kontakte sortiert nach Nachname, dann Vorname.
     */
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
}
