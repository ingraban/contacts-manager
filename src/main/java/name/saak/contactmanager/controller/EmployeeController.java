package name.saak.contactmanager.controller;

import name.saak.contactmanager.domain.Contact;
import name.saak.contactmanager.domain.Department;
import name.saak.contactmanager.domain.Employee;
import name.saak.contactmanager.service.ContactService;
import name.saak.contactmanager.service.DepartmentService;
import name.saak.contactmanager.service.EmployeeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalTime;
import java.util.List;
import java.beans.PropertyEditorSupport;

@Controller
@RequestMapping("/employees")
public class EmployeeController {

    private static final Logger log = LoggerFactory.getLogger(EmployeeController.class);

    private final EmployeeService employeeService;
    private final ContactService contactService;
    private final DepartmentService departmentService;

    public EmployeeController(EmployeeService employeeService,
                             ContactService contactService,
                             DepartmentService departmentService) {
        this.employeeService = employeeService;
        this.contactService = contactService;
        this.departmentService = departmentService;
    }

    /**
     * Registriert einen Custom Editor für LocalTime-Felder.
     */
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(LocalTime.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                if (text == null || text.trim().isEmpty()) {
                    setValue(null);
                } else {
                    setValue(LocalTime.parse(text));
                }
            }

            @Override
            public String getAsText() {
                LocalTime value = (LocalTime) getValue();
                return (value != null) ? value.toString() : "";
            }
        });
    }

    /**
     * Zeigt die Liste aller Employees.
     */
    @GetMapping
    public String listEmployees(Model model) {
        List<Employee> employees = employeeService.findAllEmployees();
        model.addAttribute("employees", employees);
        return "employees/list";
    }

    /**
     * Zeigt das Formular für einen neuen Employee.
     */
    @GetMapping("/new")
    public String showNewEmployeeForm(Model model) {
        Employee employee = new Employee();
        // Set default values
        employee.setWeeklyHours(40);

        model.addAttribute("employee", employee);
        model.addAttribute("isEdit", false);
        model.addAttribute("contacts", contactService.findAllContacts());
        model.addAttribute("departments", departmentService.findAllDepartments());
        return "employees/form";
    }

    /**
     * Erstellt einen neuen Employee.
     */
    @PostMapping
    public String createEmployee(@ModelAttribute Employee employee,
                                 @RequestParam Long contactId,
                                 @RequestParam Long departmentId,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        log.debug("Creating employee - contactId: {}, departmentId: {}, weeklyHours: {}",
                  contactId, departmentId, employee.getWeeklyHours());

        // Manual validation for weeklyHours
        if (employee.getWeeklyHours() == null || employee.getWeeklyHours() < 1) {
            model.addAttribute("errorMessage", "Wochenstunden sind erforderlich und müssen mindestens 1 sein.");
            model.addAttribute("isEdit", false);
            model.addAttribute("contacts", contactService.findAllContacts());
            model.addAttribute("departments", departmentService.findAllDepartments());
            return "employees/form";
        }

        try {
            Employee created = employeeService.createEmployee(contactId, departmentId, employee);
            redirectAttributes.addFlashAttribute("successMessage",
                "Mitarbeiter '" + created.getContact().getVorname() + " " +
                created.getContact().getNachname() + "' wurde erfolgreich erstellt.");
            return "redirect:/employees";
        } catch (EmployeeService.ContactNotFoundException |
                 DepartmentService.DepartmentNotFoundException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("isEdit", false);
            model.addAttribute("contacts", contactService.findAllContacts());
            model.addAttribute("departments", departmentService.findAllDepartments());
            return "employees/form";
        } catch (EmployeeService.DuplicateEmployeeException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("isEdit", false);
            model.addAttribute("contacts", contactService.findAllContacts());
            model.addAttribute("departments", departmentService.findAllDepartments());
            return "employees/form";
        }
    }

    /**
     * Zeigt das Formular zum Bearbeiten eines Employees.
     */
    @GetMapping("/{id}/edit")
    public String showEditEmployeeForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return employeeService.findEmployeeById(id)
            .map(employee -> {
                model.addAttribute("employee", employee);
                model.addAttribute("isEdit", true);
                model.addAttribute("departments", departmentService.findAllDepartments());
                return "employees/form";
            })
            .orElseGet(() -> {
                redirectAttributes.addFlashAttribute("errorMessage", "Mitarbeiter nicht gefunden.");
                return "redirect:/employees";
            });
    }

    /**
     * Aktualisiert einen bestehenden Employee.
     */
    @PostMapping("/{id}")
    public String updateEmployee(@PathVariable Long id,
                                @ModelAttribute Employee employee,
                                @RequestParam Long departmentId,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        // Manual validation for weeklyHours
        if (employee.getWeeklyHours() == null || employee.getWeeklyHours() < 1) {
            model.addAttribute("errorMessage", "Wochenstunden sind erforderlich und müssen mindestens 1 sein.");
            employee.setId(id);
            model.addAttribute("isEdit", true);
            model.addAttribute("departments", departmentService.findAllDepartments());
            return "employees/form";
        }

        try {
            // Set department
            Department department = departmentService.findDepartmentById(departmentId)
                .orElseThrow(() -> new DepartmentService.DepartmentNotFoundException(
                    "Department nicht gefunden"));
            employee.setDepartment(department);

            Employee updated = employeeService.updateEmployee(id, employee);
            redirectAttributes.addFlashAttribute("successMessage",
                "Mitarbeiter '" + updated.getContact().getVorname() + " " +
                updated.getContact().getNachname() + "' wurde erfolgreich aktualisiert.");
            return "redirect:/employees";
        } catch (EmployeeService.EmployeeNotFoundException |
                 DepartmentService.DepartmentNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/employees";
        }
    }

    /**
     * Löscht einen Employee.
     */
    @PostMapping("/{id}/delete")
    public String deleteEmployee(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Employee employee = employeeService.findEmployeeById(id)
                .orElseThrow(() -> new EmployeeService.EmployeeNotFoundException(
                    "Mitarbeiter nicht gefunden"));

            Contact contact = employee.getContact();
            employeeService.deleteEmployee(id);

            redirectAttributes.addFlashAttribute("successMessage",
                "Mitarbeiter '" + contact.getVorname() + " " + contact.getNachname() +
                "' wurde erfolgreich gelöscht. Der Kontakt bleibt erhalten.");
        } catch (EmployeeService.EmployeeNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/employees";
    }
}
