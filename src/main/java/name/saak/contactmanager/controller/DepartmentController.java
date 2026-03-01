package name.saak.contactmanager.controller;

import jakarta.validation.Valid;
import name.saak.contactmanager.domain.Department;
import name.saak.contactmanager.service.DepartmentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    /**
     * Zeigt die Liste aller Departments.
     */
    @GetMapping
    public String listDepartments(@RequestParam(required = false) String search, Model model) {
        List<Department> departments;

        if (search != null && !search.trim().isEmpty()) {
            departments = departmentService.searchDepartments(search);
            model.addAttribute("searchTerm", search);
        } else {
            departments = departmentService.findAllDepartments();
        }

        model.addAttribute("departments", departments);
        return "departments/list";
    }

    /**
     * Zeigt das Formular für ein neues Department.
     */
    @GetMapping("/new")
    public String showNewDepartmentForm(Model model) {
        model.addAttribute("department", new Department());
        model.addAttribute("isEdit", false);
        return "departments/form";
    }

    /**
     * Erstellt ein neues Department.
     */
    @PostMapping
    public String createDepartment(@Valid @ModelAttribute Department department,
                                   BindingResult result,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("isEdit", false);
            return "departments/form";
        }

        try {
            departmentService.createDepartment(department);
            redirectAttributes.addFlashAttribute("successMessage",
                "Abteilung '" + department.getName() + "' wurde erfolgreich erstellt.");
            return "redirect:/departments";
        } catch (DepartmentService.DuplicateDepartmentException |
                 DepartmentService.DepartmentNameConflictException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("isEdit", false);
            return "departments/form";
        }
    }

    /**
     * Zeigt das Formular zum Bearbeiten eines Departments.
     */
    @GetMapping("/{id}/edit")
    public String showEditDepartmentForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return departmentService.findDepartmentById(id)
            .map(department -> {
                model.addAttribute("department", department);
                model.addAttribute("isEdit", true);
                return "departments/form";
            })
            .orElseGet(() -> {
                redirectAttributes.addFlashAttribute("errorMessage", "Abteilung nicht gefunden.");
                return "redirect:/departments";
            });
    }

    /**
     * Aktualisiert ein bestehendes Department.
     */
    @PostMapping("/{id}")
    public String updateDepartment(@PathVariable Long id,
                                  @Valid @ModelAttribute Department department,
                                  BindingResult result,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            department.setId(id);
            model.addAttribute("isEdit", true);
            return "departments/form";
        }

        try {
            departmentService.updateDepartment(id, department);
            redirectAttributes.addFlashAttribute("successMessage",
                "Abteilung '" + department.getName() + "' wurde erfolgreich aktualisiert.");
            return "redirect:/departments";
        } catch (DepartmentService.DepartmentNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/departments";
        } catch (DepartmentService.DuplicateDepartmentException |
                 DepartmentService.DepartmentNameConflictException e) {
            department.setId(id);
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("isEdit", true);
            return "departments/form";
        }
    }

    /**
     * Löscht ein Department.
     */
    @PostMapping("/{id}/delete")
    public String deleteDepartment(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Department department = departmentService.findDepartmentById(id)
                .orElseThrow(() -> new DepartmentService.DepartmentNotFoundException(
                    "Abteilung nicht gefunden"));

            departmentService.deleteDepartment(id);
            redirectAttributes.addFlashAttribute("successMessage",
                "Abteilung '" + department.getName() + "' wurde erfolgreich gelöscht.");
        } catch (DepartmentService.DepartmentNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (DepartmentService.DepartmentInUseException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/departments";
    }
}
