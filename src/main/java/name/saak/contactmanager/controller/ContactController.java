package name.saak.contactmanager.controller;

import jakarta.validation.Valid;
import name.saak.contactmanager.domain.Contact;
import name.saak.contactmanager.service.ContactService;
import name.saak.contactmanager.service.ExcelExportService;
import name.saak.contactmanager.service.HashtagService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/contacts")
public class ContactController {

    private final ContactService contactService;
    private final HashtagService hashtagService;
    private final ExcelExportService excelExportService;

    public ContactController(ContactService contactService, HashtagService hashtagService,
                            ExcelExportService excelExportService) {
        this.contactService = contactService;
        this.hashtagService = hashtagService;
        this.excelExportService = excelExportService;
    }

    /**
     * Zeigt die Kontaktliste mit optionaler Suche.
     */
    @GetMapping
    public String listContacts(
            @RequestParam(name = "search", required = false) String searchTerm,
            Model model) {
        List<Contact> contacts;

        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            contacts = contactService.searchContacts(searchTerm);
            model.addAttribute("searchTerm", searchTerm);
        } else {
            contacts = contactService.findAllContacts();
        }

        model.addAttribute("contacts", contacts);
        model.addAttribute("availableHashtags", hashtagService.findActiveHashtags());
        return "contacts/list";
    }

    /**
     * Zeigt das Formular zum Erstellen eines neuen Kontakts.
     */
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("contact", new Contact());
        model.addAttribute("isEdit", false);
        model.addAttribute("availableHashtags", hashtagService.findActiveHashtags());
        return "contacts/form";
    }

    /**
     * Speichert einen neuen Kontakt.
     */
    @PostMapping
    public String createContact(
            @Valid @ModelAttribute("contact") Contact contact,
            BindingResult bindingResult,
            @RequestParam(name = "hashtagIds", required = false) Set<Long> hashtagIds,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", false);
            model.addAttribute("availableHashtags", hashtagService.findActiveHashtags());
            return "contacts/form";
        }

        try {
            contactService.createContact(contact, hashtagIds);
            redirectAttributes.addFlashAttribute("successMessage",
                "Kontakt erfolgreich erstellt");
            return "redirect:/contacts";
        } catch (ContactService.DuplicateContactException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("isEdit", false);
            model.addAttribute("availableHashtags", hashtagService.findActiveHashtags());
            return "contacts/form";
        }
    }

    /**
     * Zeigt das Formular zum Bearbeiten eines Kontakts.
     */
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Contact contact = contactService.findContactById(id)
            .orElseThrow(() -> new ContactService.ContactNotFoundException(
                "Kontakt mit ID " + id + " nicht gefunden"));

        model.addAttribute("contact", contact);
        model.addAttribute("isEdit", true);
        model.addAttribute("availableHashtags", hashtagService.findActiveHashtags());
        return "contacts/form";
    }

    /**
     * Aktualisiert einen bestehenden Kontakt.
     */
    @PostMapping("/{id}")
    public String updateContact(
            @PathVariable Long id,
            @Valid @ModelAttribute("contact") Contact contact,
            BindingResult bindingResult,
            @RequestParam(name = "hashtagIds", required = false) Set<Long> hashtagIds,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", true);
            contact.setId(id); // Preserve ID for form action
            model.addAttribute("availableHashtags", hashtagService.findActiveHashtags());
            return "contacts/form";
        }

        try {
            contactService.updateContact(id, contact, hashtagIds);
            redirectAttributes.addFlashAttribute("successMessage",
                "Kontakt erfolgreich aktualisiert");
            return "redirect:/contacts";
        } catch (ContactService.DuplicateContactException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("isEdit", true);
            contact.setId(id);
            model.addAttribute("availableHashtags", hashtagService.findActiveHashtags());
            return "contacts/form";
        } catch (ContactService.ContactNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/contacts";
        }
    }

    /**
     * Löscht einen Kontakt.
     */
    @PostMapping("/{id}/delete")
    public String deleteContact(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            contactService.deleteContact(id);
            redirectAttributes.addFlashAttribute("successMessage",
                "Kontakt erfolgreich gelöscht");
        } catch (ContactService.ContactNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/contacts";
    }

    /**
     * Exportiert ausgewählte Kontakte als Excel-Datei.
     */
    @PostMapping("/export")
    public ResponseEntity<byte[]> exportContacts(
            @RequestParam(name = "contactIds", required = false) List<Long> contactIds) {
        try {
            List<Contact> contacts;

            if (contactIds != null && !contactIds.isEmpty()) {
                // Exportiere nur ausgewählte Kontakte
                contacts = contactIds.stream()
                    .map(id -> contactService.findContactById(id).orElse(null))
                    .filter(contact -> contact != null)
                    .collect(Collectors.toList());
            } else {
                // Exportiere alle Kontakte wenn keine Auswahl
                contacts = contactService.findAllContacts();
            }

            byte[] excelData = excelExportService.exportContactsToExcel(contacts);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "kontakte.xlsx");

            return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Weist ausgewählten Kontakten einen Hashtag zu.
     */
    @PostMapping("/assign-hashtag")
    public String assignHashtagToContacts(
            @RequestParam(name = "contactIds") List<Long> contactIds,
            @RequestParam(name = "hashtagId") Long hashtagId,
            RedirectAttributes redirectAttributes) {
        try {
            contactService.assignHashtagToContacts(contactIds, hashtagId);
            redirectAttributes.addFlashAttribute("successMessage",
                "Hashtag erfolgreich " + contactIds.size() + " Kontakt(en) zugewiesen");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Fehler beim Zuweisen des Hashtags: " + e.getMessage());
        }
        return "redirect:/contacts";
    }

    /**
     * Entfernt einen Hashtag von ausgewählten Kontakten.
     */
    @PostMapping("/remove-hashtag")
    public String removeHashtagFromContacts(
            @RequestParam(name = "contactIds") List<Long> contactIds,
            @RequestParam(name = "hashtagId") Long hashtagId,
            RedirectAttributes redirectAttributes) {
        try {
            contactService.removeHashtagFromContacts(contactIds, hashtagId);
            redirectAttributes.addFlashAttribute("successMessage",
                "Hashtag erfolgreich von " + contactIds.size() + " Kontakt(en) entfernt");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Fehler beim Entfernen des Hashtags: " + e.getMessage());
        }
        return "redirect:/contacts";
    }

    /**
     * Exception handler für ContactNotFoundException.
     */
    @ExceptionHandler(ContactService.ContactNotFoundException.class)
    public String handleContactNotFound(
            ContactService.ContactNotFoundException e,
            RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        return "redirect:/contacts";
    }
}
