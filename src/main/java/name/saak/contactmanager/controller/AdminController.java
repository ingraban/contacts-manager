package name.saak.contactmanager.controller;

import name.saak.contactmanager.domain.User;
import name.saak.contactmanager.service.RoleService;
import name.saak.contactmanager.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Set;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final RoleService roleService;

    public AdminController(UserService userService, RoleService roleService) {
        this.userService = userService;
        this.roleService = roleService;
    }

    /**
     * Zeigt die Benutzerliste.
     */
    @GetMapping("/users")
    public String listUsers(Model model) {
        model.addAttribute("users", userService.findAllUsers());
        return "admin/users";
    }

    /**
     * Zeigt das Formular zum Bearbeiten eines Benutzers.
     */
    @GetMapping("/users/{id}/edit")
    public String showEditUserForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        User user = userService.findUserById(id)
            .orElseThrow(() -> new UserService.UserNotFoundException("Benutzer nicht gefunden"));

        model.addAttribute("user", user);
        model.addAttribute("availableRoles", roleService.findAllRoles());
        return "admin/user-form";
    }

    /**
     * Aktualisiert die Rollen, den Status und optional das Passwort eines Benutzers.
     */
    @PostMapping("/users/{id}")
    public String updateUser(@PathVariable Long id,
                            @RequestParam(name = "roleIds", required = false) Set<Long> roleIds,
                            @RequestParam(name = "enabled", required = false) Boolean enabled,
                            @RequestParam(name = "password", required = false) String password,
                            RedirectAttributes redirectAttributes) {
        try {
            if (roleIds == null || roleIds.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage",
                    "Benutzer muss mindestens eine Rolle haben");
                return "redirect:/admin/users/" + id + "/edit";
            }

            // Passwort aktualisieren, falls angegeben (nur für LOCAL-Benutzer)
            if (password != null && !password.trim().isEmpty()) {
                try {
                    userService.updatePassword(id, password);
                } catch (IllegalArgumentException e) {
                    redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
                    return "redirect:/admin/users/" + id + "/edit";
                }
            }

            // Wenn enabled null ist (sollte nicht passieren, aber zur Sicherheit), als false interpretieren
            Boolean effectiveEnabled = (enabled != null) ? enabled : false;
            userService.updateUserRoles(id, roleIds, effectiveEnabled);
            redirectAttributes.addFlashAttribute("successMessage",
                "Benutzerrechte erfolgreich aktualisiert");
            return "redirect:/admin/users";
        } catch (UserService.UserNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/users";
        } catch (UserService.LastAdminException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/users/" + id + "/edit";
        }
    }

    /**
     * Löscht einen Benutzer.
     */
    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("successMessage",
                "Benutzer erfolgreich gelöscht");
        } catch (UserService.UserNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (UserService.LastAdminException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    /**
     * Exception handler für UserNotFoundException.
     */
    @ExceptionHandler(UserService.UserNotFoundException.class)
    public String handleUserNotFound(UserService.UserNotFoundException e,
                                     RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        return "redirect:/admin/users";
    }

    /**
     * Exception handler für LastAdminException.
     */
    @ExceptionHandler(UserService.LastAdminException.class)
    public String handleLastAdmin(UserService.LastAdminException e,
                                  RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        return "redirect:/admin/users";
    }
}
