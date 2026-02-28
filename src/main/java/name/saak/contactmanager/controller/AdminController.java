package name.saak.contactmanager.controller;

import name.saak.contactmanager.domain.User;
import name.saak.contactmanager.service.DatabaseBackupService;
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
    private final DatabaseBackupService backupService;

    public AdminController(UserService userService, RoleService roleService,
                          DatabaseBackupService backupService) {
        this.userService = userService;
        this.roleService = roleService;
        this.backupService = backupService;
    }

    /**
     * Zeigt die Admin-Startseite.
     */
    @GetMapping("")
    public String adminIndex() {
        return "admin/index";
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

    // ==================== Datenbank-Verwaltung ====================

    /**
     * Zeigt die Datenbank-Verwaltungsseite.
     */
    @GetMapping("/database")
    public String showDatabasePage(Model model) {
        model.addAttribute("lastBackup", backupService.getLastBackupInfo().orElse(null));
        model.addAttribute("backups", backupService.listBackups());
        return "admin/database";
    }

    /**
     * Erstellt ein neues Datenbank-Backup.
     */
    @PostMapping("/database/backup")
    public String createBackup(RedirectAttributes redirectAttributes) {
        try {
            var backupFile = backupService.createBackup();
            redirectAttributes.addFlashAttribute("successMessage",
                "Backup erfolgreich erstellt: " + backupFile.getFileName());
        } catch (DatabaseBackupService.BackupException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Fehler beim Erstellen des Backups: " + e.getMessage());
        }
        return "redirect:/admin/database";
    }

    /**
     * Stellt die Datenbank aus einem Backup wieder her.
     */
    @PostMapping("/database/restore")
    public String restoreBackup(@RequestParam String filename,
                               RedirectAttributes redirectAttributes) {
        try {
            var backupInfo = backupService.listBackups().stream()
                .filter(b -> b.filename().equals(filename))
                .findFirst()
                .orElseThrow(() -> new DatabaseBackupService.BackupException(
                    "Backup nicht gefunden: " + filename));

            backupService.restoreBackup(backupInfo.path());
            redirectAttributes.addFlashAttribute("successMessage",
                "Datenbank erfolgreich wiederhergestellt aus: " + filename);
        } catch (DatabaseBackupService.BackupException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Fehler beim Wiederherstellen: " + e.getMessage());
        }
        return "redirect:/admin/database";
    }
}
