package name.saak.contactmanager.repository;

import name.saak.contactmanager.domain.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, LocalDate> {

    /**
     * Findet alle Feiertage sortiert nach Datum (neueste zuerst).
     */
    List<Holiday> findAllByOrderByDateDesc();

    /**
     * Findet Feiertage in einem bestimmten Jahr.
     */
    @Query("SELECT h FROM Holiday h WHERE YEAR(h.date) = :year ORDER BY h.date DESC")
    List<Holiday> findByYear(@Param("year") int year);

    /**
     * Sucht Feiertage anhand eines Suchbegriffs (Name oder Beschreibung).
     */
    @Query("SELECT h FROM Holiday h WHERE " +
           "LOWER(h.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(COALESCE(h.beschreibung, '')) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY h.date DESC")
    List<Holiday> searchHolidays(@Param("searchTerm") String searchTerm);

    /**
     * Prüft ob ein Feiertag an einem bestimmten Datum existiert (außer der angegebenen ID).
     * Wird für Unique-Constraint-Validierung beim Update verwendet.
     */
    boolean existsByDateAndDateNot(LocalDate date, LocalDate excludeDate);
}
