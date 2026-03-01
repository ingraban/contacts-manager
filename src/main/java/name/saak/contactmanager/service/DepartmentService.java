package name.saak.contactmanager.service;

import name.saak.contactmanager.domain.Department;
import name.saak.contactmanager.repository.DepartmentRepository;
import name.saak.contactmanager.repository.HashtagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final HashtagRepository hashtagRepository;

    public DepartmentService(DepartmentRepository departmentRepository,
                           HashtagRepository hashtagRepository) {
        this.departmentRepository = departmentRepository;
        this.hashtagRepository = hashtagRepository;
    }

    /**
     * Gibt alle Departments sortiert zurück.
     */
    @Transactional(readOnly = true)
    public List<Department> findAllDepartments() {
        return departmentRepository.findAllWithEmployees();
    }

    /**
     * Sucht ein Department anhand der ID.
     */
    @Transactional(readOnly = true)
    public Optional<Department> findDepartmentById(Long id) {
        return departmentRepository.findById(id);
    }

    /**
     * Sucht Departments anhand eines Suchbegriffs.
     * Gibt alle Departments zurück wenn der Suchbegriff leer ist.
     */
    @Transactional(readOnly = true)
    public List<Department> searchDepartments(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return findAllDepartments();
        }
        return departmentRepository.searchDepartmentsWithEmployees(searchTerm.trim());
    }

    /**
     * Speichert ein neues Department.
     *
     * @throws DuplicateDepartmentException wenn ein Department mit gleichem Namen existiert
     * @throws DepartmentNameConflictException wenn ein Hashtag mit #<name> existiert
     */
    public Department createDepartment(Department department) {
        validateUniqueDepartmentName(department.getName(), null);
        validateNoHashtagConflict(department.getName());
        normalizeEmptyFields(department);
        return departmentRepository.save(department);
    }

    /**
     * Aktualisiert ein bestehendes Department.
     *
     * @throws DepartmentNotFoundException wenn das Department nicht gefunden wird
     * @throws DuplicateDepartmentException wenn ein Department mit gleichem Namen existiert
     * @throws DepartmentNameConflictException wenn ein Hashtag mit #<name> existiert
     */
    public Department updateDepartment(Long id, Department updatedDepartment) {
        Department existing = departmentRepository.findById(id)
            .orElseThrow(() -> new DepartmentNotFoundException("Department mit ID " + id + " nicht gefunden"));

        validateUniqueDepartmentName(updatedDepartment.getName(), id);

        // Only validate hashtag conflict if name is changing
        if (!existing.getName().equalsIgnoreCase(updatedDepartment.getName())) {
            validateNoHashtagConflict(updatedDepartment.getName());
        }

        normalizeEmptyFields(updatedDepartment);

        // Update fields
        existing.setName(updatedDepartment.getName());
        existing.setBeschreibung(updatedDepartment.getBeschreibung());

        return departmentRepository.save(existing);
    }

    /**
     * Löscht ein Department.
     * Prüft vorher, ob noch Employees zugeordnet sind.
     *
     * @throws DepartmentNotFoundException wenn das Department nicht gefunden wird
     * @throws DepartmentInUseException wenn das Department noch Employees hat
     */
    public void deleteDepartment(Long id) {
        Department department = departmentRepository.findByIdWithEmployees(id)
            .orElseThrow(() -> new DepartmentNotFoundException("Department mit ID " + id + " nicht gefunden"));

        // Prüfe ob noch Mitarbeiter zugeordnet sind
        long employeeCount = department.getEmployees().size();
        if (employeeCount > 0) {
            throw new DepartmentInUseException(
                "Die Abteilung '" + department.getName() + "' kann nicht gelöscht werden, " +
                "da noch " + employeeCount + " Mitarbeiter zugeordnet " +
                (employeeCount == 1 ? "ist" : "sind") + ". " +
                "Bitte ordnen Sie die Mitarbeiter zuerst einer anderen Abteilung zu oder löschen Sie sie."
            );
        }

        departmentRepository.deleteById(id);
    }

    /**
     * Normalisiert leere Felder zu null.
     */
    private void normalizeEmptyFields(Department department) {
        if (department.getBeschreibung() != null && department.getBeschreibung().trim().isEmpty()) {
            department.setBeschreibung(null);
        }
    }

    /**
     * Validiert dass der Department-Name eindeutig ist.
     *
     * @param name Name des Departments
     * @param excludeId ID die ausgeschlossen werden soll (bei Updates)
     * @throws DuplicateDepartmentException wenn ein Department mit diesem Namen existiert
     */
    private void validateUniqueDepartmentName(String name, Long excludeId) {
        if (name == null || name.trim().isEmpty()) {
            return;
        }

        // Bei Updates: existierendes Department ausschließen
        if (excludeId != null) {
            if (departmentRepository.existsByNameIgnoreCaseAndIdNot(name, excludeId)) {
                throw new DuplicateDepartmentException(
                    "Ein Department mit dem Namen '" + name + "' existiert bereits"
                );
            }
        } else {
            // Bei neuen Departments: prüfen ob Name schon existiert
            Optional<Department> duplicate = departmentRepository.findByNameIgnoreCase(name);
            if (duplicate.isPresent()) {
                throw new DuplicateDepartmentException(
                    "Ein Department mit dem Namen '" + name + "' existiert bereits"
                );
            }
        }
    }

    /**
     * Validiert dass kein Hashtag mit #<departmentName> existiert.
     * Optional - kann bei Bedarf aktiviert werden für UI-Konsistenz.
     *
     * @param name Name des Departments
     * @throws DepartmentNameConflictException wenn ein Hashtag mit diesem Namen existiert
     */
    private void validateNoHashtagConflict(String name) {
        if (name == null || name.trim().isEmpty()) {
            return;
        }

        String hashtagName = "#" + name.toLowerCase();
        if (hashtagRepository.findByNameIgnoreCase(hashtagName).isPresent()) {
            throw new DepartmentNameConflictException(
                "Ein Hashtag mit dem Namen '" + hashtagName + "' existiert bereits. " +
                "Bitte wählen Sie einen anderen Department-Namen."
            );
        }
    }

    /**
     * Exception für nicht gefundene Departments.
     */
    public static class DepartmentNotFoundException extends RuntimeException {
        private static final long serialVersionUID = 202601221500L;

        public DepartmentNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Exception für doppelte Departments.
     */
    public static class DuplicateDepartmentException extends RuntimeException {
        private static final long serialVersionUID = 202601221501L;

        public DuplicateDepartmentException(String message) {
            super(message);
        }
    }

    /**
     * Exception für Department-Name-Konflikt mit Hashtag.
     */
    public static class DepartmentNameConflictException extends RuntimeException {
        private static final long serialVersionUID = 202601221502L;

        public DepartmentNameConflictException(String message) {
            super(message);
        }
    }

    /**
     * Exception wenn Department nicht gelöscht werden kann weil es noch in Benutzung ist.
     */
    public static class DepartmentInUseException extends RuntimeException {
        private static final long serialVersionUID = 202601221503L;

        public DepartmentInUseException(String message) {
            super(message);
        }
    }
}
