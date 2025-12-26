package name.saak.contactmanager.controller;

import jakarta.validation.Valid;
import name.saak.contactmanager.domain.Hashtag;
import name.saak.contactmanager.service.HashtagService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/hashtags")
public class HashtagController {

    private final HashtagService hashtagService;

    public HashtagController(HashtagService hashtagService) {
        this.hashtagService = hashtagService;
    }

    /**
     * Zeigt die Hashtag-Liste mit optionaler Suche.
     */
    @GetMapping
    public String listHashtags(
            @RequestParam(name = "search", required = false) String searchTerm,
            Model model) {
        List<Hashtag> hashtags;

        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            hashtags = hashtagService.searchHashtags(searchTerm);
            model.addAttribute("searchTerm", searchTerm);
        } else {
            hashtags = hashtagService.findAllHashtags();
        }

        model.addAttribute("hashtags", hashtags);
        return "hashtags/list";
    }

    /**
     * Zeigt das Formular zum Erstellen eines neuen Hashtags.
     */
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("hashtag", new Hashtag());
        model.addAttribute("isEdit", false);
        return "hashtags/form";
    }

    /**
     * Speichert ein neues Hashtag.
     */
    @PostMapping
    public String createHashtag(
            @Valid @ModelAttribute("hashtag") Hashtag hashtag,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", false);
            return "hashtags/form";
        }

        try {
            hashtagService.createHashtag(hashtag);
            redirectAttributes.addFlashAttribute("successMessage",
                "Hashtag erfolgreich erstellt");
            return "redirect:/hashtags";
        } catch (HashtagService.DuplicateHashtagException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("isEdit", false);
            return "hashtags/form";
        }
    }

    /**
     * Zeigt das Formular zum Bearbeiten eines Hashtags.
     */
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Hashtag hashtag = hashtagService.findHashtagById(id)
            .orElseThrow(() -> new HashtagService.HashtagNotFoundException(
                "Hashtag mit ID " + id + " nicht gefunden"));

        model.addAttribute("hashtag", hashtag);
        model.addAttribute("isEdit", true);
        return "hashtags/form";
    }

    /**
     * Aktualisiert ein bestehendes Hashtag.
     */
    @PostMapping("/{id}")
    public String updateHashtag(
            @PathVariable Long id,
            @Valid @ModelAttribute("hashtag") Hashtag hashtag,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", true);
            hashtag.setId(id); // Preserve ID for form action
            return "hashtags/form";
        }

        try {
            hashtagService.updateHashtag(id, hashtag);
            redirectAttributes.addFlashAttribute("successMessage",
                "Hashtag erfolgreich aktualisiert");
            return "redirect:/hashtags";
        } catch (HashtagService.DuplicateHashtagException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("isEdit", true);
            hashtag.setId(id);
            return "hashtags/form";
        } catch (HashtagService.HashtagNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/hashtags";
        }
    }

    /**
     * Sperrt ein Hashtag (Soft Delete).
     */
    @PostMapping("/{id}/lock")
    public String lockHashtag(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            hashtagService.lockHashtag(id);
            redirectAttributes.addFlashAttribute("successMessage",
                "Hashtag erfolgreich gesperrt");
        } catch (HashtagService.HashtagNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/hashtags";
    }

    /**
     * Entsperrt ein Hashtag.
     */
    @PostMapping("/{id}/unlock")
    public String unlockHashtag(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            hashtagService.unlockHashtag(id);
            redirectAttributes.addFlashAttribute("successMessage",
                "Hashtag erfolgreich entsperrt");
        } catch (HashtagService.HashtagNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/hashtags";
    }

    /**
     * Exception handler für HashtagNotFoundException.
     */
    @ExceptionHandler(HashtagService.HashtagNotFoundException.class)
    public String handleHashtagNotFound(
            HashtagService.HashtagNotFoundException e,
            RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        return "redirect:/hashtags";
    }
}
