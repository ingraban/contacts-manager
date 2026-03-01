package name.saak.contactmanager.repository;

import name.saak.contactmanager.domain.Employee;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    /**
     * Findet einen Employee anhand der Contact-ID mit eager loading von Contact und Department.
     */
    @EntityGraph(attributePaths = {"contact", "department"})
    Optional<Employee> findByContact_Id(Long contactId);

    /**
     * Findet alle Employees eines Departments sortiert nach Contact-Namen mit eager loading.
     */
    @EntityGraph(attributePaths = {"contact", "department"})
    List<Employee> findByDepartment_IdOrderByContact_NachnameAscContact_VornameAsc(Long departmentId);

    /**
     * Findet alle Employees sortiert nach Contact-Namen mit eager loading.
     */
    @EntityGraph(attributePaths = {"contact", "department"})
    List<Employee> findAllByOrderByContact_NachnameAscContact_VornameAsc();

    /**
     * Prüft ob ein Contact bereits ein Employee ist.
     */
    boolean existsByContact_Id(Long contactId);

    /**
     * Findet einen Employee anhand der ID mit eager loading von Contact und Department.
     */
    @EntityGraph(attributePaths = {"contact", "department"})
    Optional<Employee> findById(Long id);
}
