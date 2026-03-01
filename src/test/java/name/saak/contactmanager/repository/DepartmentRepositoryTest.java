package name.saak.contactmanager.repository;

import name.saak.contactmanager.domain.Department;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class DepartmentRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Test
    void shouldSaveDepartment() {
        // Given
        Department department = new Department("IT", "Informationstechnologie");

        // When
        Department saved = departmentRepository.save(department);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("IT");
        assertThat(saved.getBeschreibung()).isEqualTo("Informationstechnologie");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldFindByNameIgnoreCase() {
        // Given
        Department department = new Department("Verwaltung");
        entityManager.persist(department);
        entityManager.flush();

        // When
        Optional<Department> found = departmentRepository.findByNameIgnoreCase("verwaltung");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Verwaltung");
    }

    @Test
    void shouldFindAllOrderedByName() {
        // Given
        entityManager.persist(new Department("Vertrieb"));
        entityManager.persist(new Department("IT"));
        entityManager.persist(new Department("Verwaltung"));
        entityManager.flush();

        // When
        List<Department> departments = departmentRepository.findAllByOrderByNameAsc();

        // Then
        assertThat(departments).hasSize(3);
        assertThat(departments.get(0).getName()).isEqualTo("IT");
        assertThat(departments.get(1).getName()).isEqualTo("Vertrieb");
        assertThat(departments.get(2).getName()).isEqualTo("Verwaltung");
    }

    @Test
    void shouldSearchDepartmentsByName() {
        // Given
        entityManager.persist(new Department("IT Support"));
        entityManager.persist(new Department("IT Development"));
        entityManager.persist(new Department("Verwaltung"));
        entityManager.flush();

        // When
        List<Department> results = departmentRepository.searchDepartments("IT");

        // Then
        assertThat(results).hasSize(2);
        assertThat(results).extracting(Department::getName)
            .containsExactly("IT Development", "IT Support");
    }

    @Test
    void shouldSearchDepartmentsByBeschreibung() {
        // Given
        entityManager.persist(new Department("IT", "Technologie und Support"));
        entityManager.persist(new Department("HR", "Personal und Technologie"));
        entityManager.persist(new Department("Sales", "Vertrieb"));
        entityManager.flush();

        // When
        List<Department> results = departmentRepository.searchDepartments("Technologie");

        // Then
        assertThat(results).hasSize(2);
        assertThat(results).extracting(Department::getName)
            .containsExactlyInAnyOrder("IT", "HR");
    }

    @Test
    void shouldCheckExistsByNameIgnoreCaseAndIdNot() {
        // Given
        Department department = new Department("IT");
        department = entityManager.persist(department);
        entityManager.flush();

        // When/Then
        assertThat(departmentRepository.existsByNameIgnoreCaseAndIdNot("IT", 999L)).isTrue();
        assertThat(departmentRepository.existsByNameIgnoreCaseAndIdNot("IT", department.getId())).isFalse();
        assertThat(departmentRepository.existsByNameIgnoreCaseAndIdNot("NonExistent", department.getId())).isFalse();
    }

    @Test
    void shouldEnforceUniqueConstraintOnName() {
        // Given
        Department dept1 = new Department("IT");
        entityManager.persist(dept1);
        entityManager.flush();

        // When/Then - should throw exception on duplicate name
        Department dept2 = new Department("IT");
        try {
            entityManager.persist(dept2);
            entityManager.flush();
            assertThat(false).as("Should have thrown constraint violation").isTrue();
        } catch (Exception e) {
            // Expected - unique constraint violation
            assertThat(e).isNotNull();
        }
    }
}
