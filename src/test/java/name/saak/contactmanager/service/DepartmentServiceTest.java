package name.saak.contactmanager.service;

import name.saak.contactmanager.domain.Department;
import name.saak.contactmanager.domain.Hashtag;
import name.saak.contactmanager.repository.DepartmentRepository;
import name.saak.contactmanager.repository.HashtagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private HashtagRepository hashtagRepository;

    @InjectMocks
    private DepartmentService departmentService;

    private Department department;

    @BeforeEach
    void setUp() {
        department = new Department("IT", "Informationstechnologie");
        department.setId(1L);
    }

    @Test
    void shouldFindAllDepartments() {
        // Given
        when(departmentRepository.findAllWithEmployees()).thenReturn(List.of(department));

        // When
        List<Department> result = departmentService.findAllDepartments();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("IT");
        verify(departmentRepository).findAllWithEmployees();
    }

    @Test
    void shouldFindDepartmentById() {
        // Given
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));

        // When
        Optional<Department> result = departmentService.findDepartmentById(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("IT");
    }

    @Test
    void shouldSearchDepartments() {
        // Given
        when(departmentRepository.searchDepartmentsWithEmployees("IT")).thenReturn(List.of(department));

        // When
        List<Department> result = departmentService.searchDepartments("IT");

        // Then
        assertThat(result).hasSize(1);
        verify(departmentRepository).searchDepartmentsWithEmployees("IT");
    }

    @Test
    void shouldReturnAllDepartmentsWhenSearchTermIsEmpty() {
        // Given
        when(departmentRepository.findAllWithEmployees()).thenReturn(List.of(department));

        // When
        List<Department> result = departmentService.searchDepartments("");

        // Then
        assertThat(result).hasSize(1);
        verify(departmentRepository).findAllWithEmployees();
        verify(departmentRepository, never()).searchDepartmentsWithEmployees(anyString());
    }

    @Test
    void shouldCreateDepartment() {
        // Given
        Department newDept = new Department("Verwaltung");
        when(departmentRepository.findByNameIgnoreCase("Verwaltung")).thenReturn(Optional.empty());
        when(hashtagRepository.findByNameIgnoreCase("#verwaltung")).thenReturn(Optional.empty());
        when(departmentRepository.save(any(Department.class))).thenReturn(newDept);

        // When
        Department result = departmentService.createDepartment(newDept);

        // Then
        assertThat(result).isNotNull();
        verify(departmentRepository).save(newDept);
    }

    @Test
    void shouldThrowExceptionWhenCreatingDuplicateDepartment() {
        // Given
        when(departmentRepository.findByNameIgnoreCase("IT")).thenReturn(Optional.of(department));

        // When/Then
        assertThatThrownBy(() -> departmentService.createDepartment(new Department("IT")))
            .isInstanceOf(DepartmentService.DuplicateDepartmentException.class)
            .hasMessageContaining("existiert bereits");

        verify(departmentRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenHashtagConflictOnCreate() {
        // Given
        Hashtag hashtag = new Hashtag("#it");
        when(departmentRepository.findByNameIgnoreCase("IT")).thenReturn(Optional.empty());
        when(hashtagRepository.findByNameIgnoreCase("#it")).thenReturn(Optional.of(hashtag));

        // When/Then
        assertThatThrownBy(() -> departmentService.createDepartment(new Department("IT")))
            .isInstanceOf(DepartmentService.DepartmentNameConflictException.class)
            .hasMessageContaining("Hashtag");

        verify(departmentRepository, never()).save(any());
    }

    @Test
    void shouldUpdateDepartment() {
        // Given
        Department updatedDept = new Department("IT Support");
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(departmentRepository.existsByNameIgnoreCaseAndIdNot("IT Support", 1L)).thenReturn(false);
        when(hashtagRepository.findByNameIgnoreCase("#it support")).thenReturn(Optional.empty());
        when(departmentRepository.save(any(Department.class))).thenReturn(department);

        // When
        Department result = departmentService.updateDepartment(1L, updatedDept);

        // Then
        assertThat(result).isNotNull();
        verify(departmentRepository).save(department);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentDepartment() {
        // Given
        when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> departmentService.updateDepartment(999L, new Department("Test")))
            .isInstanceOf(DepartmentService.DepartmentNotFoundException.class)
            .hasMessageContaining("nicht gefunden");

        verify(departmentRepository, never()).save(any());
    }

    @Test
    void shouldDeleteDepartment() {
        // Given
        when(departmentRepository.existsById(1L)).thenReturn(true);
        doNothing().when(departmentRepository).deleteById(1L);

        // When
        departmentService.deleteDepartment(1L);

        // Then
        verify(departmentRepository).deleteById(1L);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentDepartment() {
        // Given
        when(departmentRepository.existsById(999L)).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> departmentService.deleteDepartment(999L))
            .isInstanceOf(DepartmentService.DepartmentNotFoundException.class);

        verify(departmentRepository, never()).deleteById(any());
    }

    @Test
    void shouldThrowExceptionWhenDeletingDepartmentInUse() {
        // Given
        when(departmentRepository.existsById(1L)).thenReturn(true);
        doThrow(new DataIntegrityViolationException("FK violation"))
            .when(departmentRepository).deleteById(1L);

        // When/Then
        assertThatThrownBy(() -> departmentService.deleteDepartment(1L))
            .isInstanceOf(DepartmentService.DepartmentInUseException.class)
            .hasMessageContaining("Mitarbeiter");
    }
}
