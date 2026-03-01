package name.saak.contactmanager.repository;

import name.saak.contactmanager.domain.Contact;
import name.saak.contactmanager.domain.Department;
import name.saak.contactmanager.domain.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class EmployeeRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EmployeeRepository employeeRepository;

    private Department department;
    private Contact contact1;
    private Contact contact2;

    @BeforeEach
    void setUp() {
        // Create test department with unique name
        department = new Department("IT Test Dept");
        department = entityManager.persist(department);

        // Create test contacts
        contact1 = new Contact("Max", "Mustermann", "Teststr. 1", "12345", "Berlin");
        contact1 = entityManager.persist(contact1);

        contact2 = new Contact("Anna", "Schmidt", "Teststr. 2", "12345", "Berlin");
        contact2 = entityManager.persist(contact2);

        entityManager.flush();
    }

    @Test
    void shouldSaveEmployee() {
        // Given
        Employee employee = new Employee(contact1, department, 40);

        // When
        Employee saved = employeeRepository.save(employee);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getContact()).isEqualTo(contact1);
        assertThat(saved.getDepartment()).isEqualTo(department);
        assertThat(saved.getWeeklyHours()).isEqualTo(40);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldFindByContactId() {
        // Given
        Employee employee = new Employee(contact1, department, 40);
        employee = entityManager.persist(employee);
        entityManager.flush();

        // When
        Optional<Employee> found = employeeRepository.findByContact_Id(contact1.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getContact().getId()).isEqualTo(contact1.getId());
        assertThat(found.get().getDepartment()).isNotNull(); // Eager loaded
    }

    @Test
    void shouldFindByDepartmentIdOrderedByName() {
        // Given
        Employee emp1 = new Employee(contact2, department, 40); // Anna Schmidt
        Employee emp2 = new Employee(contact1, department, 40); // Max Mustermann
        entityManager.persist(emp1);
        entityManager.persist(emp2);
        entityManager.flush();

        // When
        List<Employee> employees = employeeRepository
            .findByDepartment_IdOrderByContact_NachnameAscContact_VornameAsc(department.getId());

        // Then
        assertThat(employees).hasSize(2);
        assertThat(employees.get(0).getContact().getNachname()).isEqualTo("Mustermann");
        assertThat(employees.get(1).getContact().getNachname()).isEqualTo("Schmidt");
    }

    @Test
    void shouldFindAllOrderedByContactName() {
        // Given
        Department dept2 = entityManager.persist(new Department("Verwaltung Test Dept"));
        Employee emp1 = new Employee(contact2, department, 40); // Anna Schmidt, IT Test Dept
        Employee emp2 = new Employee(contact1, dept2, 40); // Max Mustermann, Verwaltung Test Dept
        entityManager.persist(emp1);
        entityManager.persist(emp2);
        entityManager.flush();

        // When
        List<Employee> employees = employeeRepository.findAllByOrderByContact_NachnameAscContact_VornameAsc();

        // Then
        assertThat(employees).hasSize(2);
        assertThat(employees.get(0).getContact().getNachname()).isEqualTo("Mustermann");
        assertThat(employees.get(1).getContact().getNachname()).isEqualTo("Schmidt");
    }

    @Test
    void shouldCheckExistsByContactId() {
        // Given
        Employee employee = new Employee(contact1, department, 40);
        entityManager.persist(employee);
        entityManager.flush();

        // When/Then
        assertThat(employeeRepository.existsByContact_Id(contact1.getId())).isTrue();
        assertThat(employeeRepository.existsByContact_Id(contact2.getId())).isFalse();
        assertThat(employeeRepository.existsByContact_Id(999L)).isFalse();
    }

    @Test
    void shouldEnforceUniqueConstraintOnContactId() {
        // Given
        Employee emp1 = new Employee(contact1, department, 40);
        entityManager.persist(emp1);
        entityManager.flush();

        // When/Then - should throw exception on duplicate contact
        Employee emp2 = new Employee(contact1, department, 30);
        try {
            entityManager.persist(emp2);
            entityManager.flush();
            assertThat(false).as("Should have thrown constraint violation").isTrue();
        } catch (Exception e) {
            // Expected - unique constraint violation
            assertThat(e).isNotNull();
        }
    }

    @Test
    void shouldCascadeDeleteWhenContactIsDeleted() {
        // Given
        Employee employee = new Employee(contact1, department, 40);
        employee = entityManager.persist(employee);
        entityManager.flush();
        Long employeeId = employee.getId();

        // When
        entityManager.remove(contact1);
        entityManager.flush();
        entityManager.clear();

        // Then
        assertThat(employeeRepository.findById(employeeId)).isEmpty();
    }

    @Test
    void shouldNotDeleteEmployeesWhenDepartmentIsDeleted() {
        // Given
        Employee employee = new Employee(contact1, department, 40);
        entityManager.persist(employee);
        entityManager.flush();

        // When/Then - should throw exception due to RESTRICT constraint
        try {
            entityManager.remove(department);
            entityManager.flush();
            assertThat(false).as("Should have thrown constraint violation").isTrue();
        } catch (Exception e) {
            // Expected - foreign key constraint violation (RESTRICT)
            assertThat(e).isNotNull();
        }
    }
}
