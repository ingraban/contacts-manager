package name.saak.contactmanager.service;

import name.saak.contactmanager.domain.Contact;
import name.saak.contactmanager.domain.Department;
import name.saak.contactmanager.domain.Employee;
import name.saak.contactmanager.repository.ContactRepository;
import name.saak.contactmanager.repository.DepartmentRepository;
import name.saak.contactmanager.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private EmployeeService employeeService;

    private Contact contact;
    private Department department;
    private Employee employee;

    @BeforeEach
    void setUp() {
        contact = new Contact("Max", "Mustermann", "Teststr. 1", "12345", "Berlin");
        contact.setId(1L);

        department = new Department("IT");
        department.setId(1L);

        employee = new Employee(contact, department, 40);
        employee.setId(1L);
    }

    @Test
    void shouldFindAllEmployees() {
        // Given
        when(employeeRepository.findAllByOrderByContact_NachnameAscContact_VornameAsc())
            .thenReturn(List.of(employee));

        // When
        List<Employee> result = employeeService.findAllEmployees();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContact().getNachname()).isEqualTo("Mustermann");
    }

    @Test
    void shouldFindEmployeeById() {
        // Given
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        // When
        Optional<Employee> result = employeeService.findEmployeeById(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getWeeklyHours()).isEqualTo(40);
    }

    @Test
    void shouldFindEmployeeByContactId() {
        // Given
        when(employeeRepository.findByContact_Id(1L)).thenReturn(Optional.of(employee));

        // When
        Optional<Employee> result = employeeService.findEmployeeByContactId(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getContact().getId()).isEqualTo(1L);
    }

    @Test
    void shouldFindEmployeesByDepartment() {
        // Given
        when(employeeRepository.findByDepartment_IdOrderByContact_NachnameAscContact_VornameAsc(1L))
            .thenReturn(List.of(employee));

        // When
        List<Employee> result = employeeService.findEmployeesByDepartment(1L);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDepartment().getId()).isEqualTo(1L);
    }

    @Test
    void shouldCreateEmployee() {
        // Given
        Employee employeeData = new Employee();
        employeeData.setWeeklyHours(40);
        employeeData.setWorksMonday(true);
        employeeData.setWorksFriday(true);

        when(contactRepository.findById(1L)).thenReturn(Optional.of(contact));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(employeeRepository.existsByContact_Id(1L)).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);

        // When
        Employee result = employeeService.createEmployee(1L, 1L, employeeData);

        // Then
        assertThat(result).isNotNull();
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    void shouldThrowExceptionWhenContactNotFound() {
        // Given
        when(contactRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> employeeService.createEmployee(999L, 1L, new Employee()))
            .isInstanceOf(EmployeeService.ContactNotFoundException.class)
            .hasMessageContaining("nicht gefunden");

        verify(employeeRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenDepartmentNotFound() {
        // Given
        when(contactRepository.findById(1L)).thenReturn(Optional.of(contact));
        when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> employeeService.createEmployee(1L, 999L, new Employee()))
            .isInstanceOf(DepartmentService.DepartmentNotFoundException.class)
            .hasMessageContaining("nicht gefunden");

        verify(employeeRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenContactIsAlreadyEmployee() {
        // Given
        when(contactRepository.findById(1L)).thenReturn(Optional.of(contact));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(employeeRepository.existsByContact_Id(1L)).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> employeeService.createEmployee(1L, 1L, new Employee()))
            .isInstanceOf(EmployeeService.DuplicateEmployeeException.class)
            .hasMessageContaining("bereits ein Mitarbeiter");

        verify(employeeRepository, never()).save(any());
    }

    @Test
    void shouldConvertContactToEmployee() {
        // Given
        when(contactRepository.findById(1L)).thenReturn(Optional.of(contact));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(employeeRepository.existsByContact_Id(1L)).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);

        // When
        Employee result = employeeService.convertContactToEmployee(1L, 1L, 40);

        // Then
        assertThat(result).isNotNull();
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    void shouldUpdateEmployee() {
        // Given
        Employee updatedEmployee = new Employee();
        updatedEmployee.setWeeklyHours(35);
        updatedEmployee.setDepartment(department);
        updatedEmployee.setWorksMonday(true);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);

        // When
        Employee result = employeeService.updateEmployee(1L, updatedEmployee);

        // Then
        assertThat(result).isNotNull();
        verify(employeeRepository).save(employee);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentEmployee() {
        // Given
        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> employeeService.updateEmployee(999L, new Employee()))
            .isInstanceOf(EmployeeService.EmployeeNotFoundException.class)
            .hasMessageContaining("nicht gefunden");

        verify(employeeRepository, never()).save(any());
    }

    @Test
    void shouldDeleteEmployee() {
        // Given
        when(employeeRepository.existsById(1L)).thenReturn(true);
        doNothing().when(employeeRepository).deleteById(1L);

        // When
        employeeService.deleteEmployee(1L);

        // Then
        verify(employeeRepository).deleteById(1L);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentEmployee() {
        // Given
        when(employeeRepository.existsById(999L)).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> employeeService.deleteEmployee(999L))
            .isInstanceOf(EmployeeService.EmployeeNotFoundException.class);

        verify(employeeRepository, never()).deleteById(any());
    }
}
