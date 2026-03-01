package name.saak.contactmanager.repository;

import name.saak.contactmanager.domain.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    /**
     * Findet ein Department anhand seines Namens (case-insensitive).
     */
    Optional<Department> findByNameIgnoreCase(String name);

    /**
     * Findet alle Departments sortiert nach Name.
     */
    List<Department> findAllByOrderByNameAsc();

    /**
     * Prüft ob ein Department mit dem gleichen Namen existiert (außer der angegebenen ID).
     * Wird für Unique-Constraint-Validierung beim Update verwendet.
     */
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    /**
     * Sucht Departments anhand eines Suchbegriffs.
     * Durchsucht Name und Beschreibung mit SQL LIKE (case-insensitive).
     */
    @Query("SELECT d FROM Department d WHERE " +
           "LOWER(d.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(COALESCE(d.beschreibung, '')) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY d.name ASC")
    List<Department> searchDepartments(@Param("searchTerm") String searchTerm);

    /**
     * Findet alle Departments mit ihren Employees (EAGER loading für die Liste).
     */
    @Query("SELECT DISTINCT d FROM Department d LEFT JOIN FETCH d.employees ORDER BY d.name ASC")
    List<Department> findAllWithEmployees();

    /**
     * Sucht Departments mit ihren Employees anhand eines Suchbegriffs.
     */
    @Query("SELECT DISTINCT d FROM Department d LEFT JOIN FETCH d.employees WHERE " +
           "LOWER(d.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(COALESCE(d.beschreibung, '')) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY d.name ASC")
    List<Department> searchDepartmentsWithEmployees(@Param("searchTerm") String searchTerm);

    /**
     * Findet ein Department mit seinen Employees (EAGER loading).
     */
    @Query("SELECT d FROM Department d LEFT JOIN FETCH d.employees WHERE d.id = :id")
    Optional<Department> findByIdWithEmployees(@Param("id") Long id);
}
