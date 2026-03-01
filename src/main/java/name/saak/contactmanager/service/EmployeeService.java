package name.saak.contactmanager.service;

import name.saak.contactmanager.domain.Contact;
import name.saak.contactmanager.domain.Department;
import name.saak.contactmanager.domain.Employee;
import name.saak.contactmanager.repository.ContactRepository;
import name.saak.contactmanager.repository.DepartmentRepository;
import name.saak.contactmanager.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final ContactRepository contactRepository;
    private final DepartmentRepository departmentRepository;

    public EmployeeService(EmployeeRepository employeeRepository,
                          ContactRepository contactRepository,
                          DepartmentRepository departmentRepository) {
        this.employeeRepository = employeeRepository;
        this.contactRepository = contactRepository;
        this.departmentRepository = departmentRepository;
    }

    /**
     * Gibt alle Employees sortiert nach Contact-Namen zurück.
     */
    @Transactional(readOnly = true)
    public List<Employee> findAllEmployees() {
        return employeeRepository.findAllByOrderByContact_NachnameAscContact_VornameAsc();
    }

    /**
     * Sucht einen Employee anhand der ID.
     */
    @Transactional(readOnly = true)
    public Optional<Employee> findEmployeeById(Long id) {
        return employeeRepository.findById(id);
    }

    /**
     * Sucht einen Employee anhand der Contact-ID.
     */
    @Transactional(readOnly = true)
    public Optional<Employee> findEmployeeByContactId(Long contactId) {
        return employeeRepository.findByContact_Id(contactId);
    }

    /**
     * Findet alle Employees eines Departments.
     */
    @Transactional(readOnly = true)
    public List<Employee> findEmployeesByDepartment(Long departmentId) {
        return employeeRepository.findByDepartment_IdOrderByContact_NachnameAscContact_VornameAsc(departmentId);
    }

    /**
     * Erstellt einen neuen Employee.
     *
     * @param contactId ID des zugeordneten Contacts
     * @param departmentId ID des zugeordneten Departments
     * @param employeeData Employee-Daten (weeklyHours, working days, hours)
     * @throws ContactNotFoundException wenn der Contact nicht gefunden wird
     * @throws DepartmentService.DepartmentNotFoundException wenn das Department nicht gefunden wird
     * @throws DuplicateEmployeeException wenn der Contact bereits ein Employee ist
     */
    public Employee createEmployee(Long contactId, Long departmentId, Employee employeeData) {
        // Validate contact exists
        Contact contact = contactRepository.findById(contactId)
            .orElseThrow(() -> new ContactNotFoundException("Contact mit ID " + contactId + " nicht gefunden"));

        // Validate department exists
        Department department = departmentRepository.findById(departmentId)
            .orElseThrow(() -> new DepartmentService.DepartmentNotFoundException(
                "Department mit ID " + departmentId + " nicht gefunden"));

        // Validate contact is not already an employee
        if (employeeRepository.existsByContact_Id(contactId)) {
            throw new DuplicateEmployeeException(
                "Contact '" + contact.getVorname() + " " + contact.getNachname() + "' ist bereits ein Mitarbeiter"
            );
        }

        // Create employee
        Employee employee = new Employee();
        employee.setContact(contact);
        employee.setDepartment(department);
        employee.setWeeklyHours(employeeData.getWeeklyHours());

        // Copy working days
        employee.setWorksMonday(employeeData.getWorksMonday());
        employee.setWorksTuesday(employeeData.getWorksTuesday());
        employee.setWorksWednesday(employeeData.getWorksWednesday());
        employee.setWorksThursday(employeeData.getWorksThursday());
        employee.setWorksFriday(employeeData.getWorksFriday());
        employee.setWorksSaturday(employeeData.getWorksSaturday());
        employee.setWorksSunday(employeeData.getWorksSunday());

        // Copy daily hours
        employee.setMondayHours(employeeData.getMondayHours());
        employee.setTuesdayHours(employeeData.getTuesdayHours());
        employee.setWednesdayHours(employeeData.getWednesdayHours());
        employee.setThursdayHours(employeeData.getThursdayHours());
        employee.setFridayHours(employeeData.getFridayHours());
        employee.setSaturdayHours(employeeData.getSaturdayHours());
        employee.setSundayHours(employeeData.getSundayHours());

        return employeeRepository.save(employee);
    }

    /**
     * Konvertiert einen existierenden Contact zu einem Employee.
     * Helper-Methode die createEmployee mit Standardwerten aufruft.
     *
     * @param contactId ID des zu konvertierenden Contacts
     * @param departmentId ID des zugeordneten Departments
     * @param weeklyHours Wochenstunden
     */
    public Employee convertContactToEmployee(Long contactId, Long departmentId, Integer weeklyHours) {
        Employee employeeData = new Employee();
        employeeData.setWeeklyHours(weeklyHours);
        // All boolean fields will be initialized to false by default
        return createEmployee(contactId, departmentId, employeeData);
    }

    /**
     * Aktualisiert einen bestehenden Employee.
     *
     * @param id ID des Employees
     * @param updatedEmployee Aktualisierte Employee-Daten
     * @throws EmployeeNotFoundException wenn der Employee nicht gefunden wird
     * @throws DepartmentService.DepartmentNotFoundException wenn das neue Department nicht gefunden wird
     */
    public Employee updateEmployee(Long id, Employee updatedEmployee) {
        Employee existing = employeeRepository.findById(id)
            .orElseThrow(() -> new EmployeeNotFoundException("Employee mit ID " + id + " nicht gefunden"));

        // Validate department if changed
        if (updatedEmployee.getDepartment() != null &&
            !existing.getDepartment().getId().equals(updatedEmployee.getDepartment().getId())) {
            Department department = departmentRepository.findById(updatedEmployee.getDepartment().getId())
                .orElseThrow(() -> new DepartmentService.DepartmentNotFoundException(
                    "Department mit ID " + updatedEmployee.getDepartment().getId() + " nicht gefunden"));
            existing.setDepartment(department);
        }

        // Update fields
        existing.setWeeklyHours(updatedEmployee.getWeeklyHours());

        // Update working days
        existing.setWorksMonday(updatedEmployee.getWorksMonday());
        existing.setWorksTuesday(updatedEmployee.getWorksTuesday());
        existing.setWorksWednesday(updatedEmployee.getWorksWednesday());
        existing.setWorksThursday(updatedEmployee.getWorksThursday());
        existing.setWorksFriday(updatedEmployee.getWorksFriday());
        existing.setWorksSaturday(updatedEmployee.getWorksSaturday());
        existing.setWorksSunday(updatedEmployee.getWorksSunday());

        // Update daily hours
        existing.setMondayHours(updatedEmployee.getMondayHours());
        existing.setTuesdayHours(updatedEmployee.getTuesdayHours());
        existing.setWednesdayHours(updatedEmployee.getWednesdayHours());
        existing.setThursdayHours(updatedEmployee.getThursdayHours());
        existing.setFridayHours(updatedEmployee.getFridayHours());
        existing.setSaturdayHours(updatedEmployee.getSaturdayHours());
        existing.setSundayHours(updatedEmployee.getSundayHours());

        return employeeRepository.save(existing);
    }

    /**
     * Löscht einen Employee.
     * Der zugeordnete Contact wird NICHT gelöscht (nur die Employee-Beziehung).
     *
     * @throws EmployeeNotFoundException wenn der Employee nicht gefunden wird
     */
    public void deleteEmployee(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new EmployeeNotFoundException("Employee mit ID " + id + " nicht gefunden");
        }
        employeeRepository.deleteById(id);
    }

    /**
     * Exception für nicht gefundene Contacts.
     */
    public static class ContactNotFoundException extends RuntimeException {
        private static final long serialVersionUID = 202601221510L;

        public ContactNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Exception für nicht gefundene Employees.
     */
    public static class EmployeeNotFoundException extends RuntimeException {
        private static final long serialVersionUID = 202601221511L;

        public EmployeeNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Exception wenn ein Contact bereits ein Employee ist.
     */
    public static class DuplicateEmployeeException extends RuntimeException {
        private static final long serialVersionUID = 202601221512L;

        public DuplicateEmployeeException(String message) {
            super(message);
        }
    }
}
