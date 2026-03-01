package name.saak.contactmanager.controller;

import name.saak.contactmanager.domain.Holiday;
import name.saak.contactmanager.service.HolidayImportService;
import name.saak.contactmanager.service.HolidayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/holidays")
@PreAuthorize("hasRole('PERSONAL')")
public class HolidayController {

    private static final Logger log = LoggerFactory.getLogger(HolidayController.class);

    private final HolidayService holidayService;
    private final HolidayImportService holidayImportService;

    public HolidayController(HolidayService holidayService, HolidayImportService holidayImportService) {
        this.holidayService = holidayService;
        this.holidayImportService = holidayImportService;
    }

    /**
     * Zeigt die Liste aller Feiertage mit optionaler Suchfunktion.
     */
    @GetMapping
    public String listHolidays(@RequestParam(required = false) String search,
                               @RequestParam(required = false) Integer year,
                               Model model) {
        List<Holiday> holidays;

        if (year != null) {
            holidays = holidayService.findHolidaysByYear(year);
            model.addAttribute("year", year);
        } else if (search != null && !search.trim().isEmpty()) {
            holidays = holidayService.searchHolidays(search);
            model.addAttribute("search", search);
        } else {
            holidays = holidayService.findAllHolidays();
        }

        model.addAttribute("holidays", holidays);
        return "holidays/list";
    }

    /**
     * Zeigt das Formular zum Erstellen eines neuen Feiertags.
     */
    @GetMapping("/new")
    public String showNewHolidayForm(Model model) {
        model.addAttribute("holiday", new Holiday());
        model.addAttribute("isEdit", false);
        return "holidays/form";
    }

    /**
     * Erstellt einen neuen Feiertag.
     */
    @PostMapping
    public String createHoliday(@Valid @ModelAttribute Holiday holiday,
                                BindingResult result,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (result.hasErrors()) {
            model.addAttribute("isEdit", false);
            return "holidays/form";
        }

        try {
            holidayService.createHoliday(holiday);
            redirectAttributes.addFlashAttribute("successMessage",
                "Feiertag '" + holiday.getName() + "' wurde erfolgreich erstellt.");
            return "redirect:/holidays";
        } catch (HolidayService.DuplicateHolidayException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("isEdit", false);
            return "holidays/form";
        } catch (Exception e) {
            log.error("Fehler beim Erstellen des Feiertags", e);
            model.addAttribute("errorMessage", "Fehler beim Erstellen des Feiertags: " + e.getMessage());
            model.addAttribute("isEdit", false);
            return "holidays/form";
        }
    }

    /**
     * Zeigt das Formular zum Bearbeiten eines Feiertags.
     */
    @GetMapping("/{date}/edit")
    public String showEditHolidayForm(@PathVariable String date, Model model, RedirectAttributes redirectAttributes) {
        try {
            LocalDate holidayDate = LocalDate.parse(date);
            Holiday holiday = holidayService.findHolidayByDate(holidayDate)
                .orElseThrow(() -> new HolidayService.HolidayNotFoundException("Feiertag nicht gefunden"));

            model.addAttribute("holiday", holiday);
            model.addAttribute("originalDate", date);
            model.addAttribute("isEdit", true);
            return "holidays/form";
        } catch (HolidayService.HolidayNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/holidays";
        } catch (Exception e) {
            log.error("Fehler beim Laden des Feiertags", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Fehler beim Laden des Feiertags");
            return "redirect:/holidays";
        }
    }

    /**
     * Aktualisiert einen bestehenden Feiertag.
     */
    @PostMapping("/{date}")
    public String updateHoliday(@PathVariable String date,
                                @Valid @ModelAttribute Holiday holiday,
                                BindingResult result,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (result.hasErrors()) {
            model.addAttribute("originalDate", date);
            model.addAttribute("isEdit", true);
            return "holidays/form";
        }

        try {
            LocalDate originalDate = LocalDate.parse(date);
            holidayService.updateHoliday(originalDate, holiday);
            redirectAttributes.addFlashAttribute("successMessage",
                "Feiertag '" + holiday.getName() + "' wurde erfolgreich aktualisiert.");
            return "redirect:/holidays";
        } catch (HolidayService.HolidayNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/holidays";
        } catch (HolidayService.DuplicateHolidayException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("originalDate", date);
            model.addAttribute("isEdit", true);
            return "holidays/form";
        } catch (Exception e) {
            log.error("Fehler beim Aktualisieren des Feiertags", e);
            model.addAttribute("errorMessage", "Fehler beim Aktualisieren des Feiertags: " + e.getMessage());
            model.addAttribute("originalDate", date);
            model.addAttribute("isEdit", true);
            return "holidays/form";
        }
    }

    /**
     * Löscht einen Feiertag.
     */
    @PostMapping("/{date}/delete")
    public String deleteHoliday(@PathVariable String date, RedirectAttributes redirectAttributes) {
        try {
            LocalDate holidayDate = LocalDate.parse(date);
            Holiday holiday = holidayService.findHolidayByDate(holidayDate)
                .orElseThrow(() -> new HolidayService.HolidayNotFoundException("Feiertag nicht gefunden"));

            holidayService.deleteHoliday(holidayDate);
            redirectAttributes.addFlashAttribute("successMessage",
                "Feiertag '" + holiday.getName() + "' wurde erfolgreich gelöscht.");
        } catch (HolidayService.HolidayNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Fehler beim Löschen des Feiertags", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Fehler beim Löschen des Feiertags");
        }
        return "redirect:/holidays";
    }

    /**
     * Importiert Feiertage für das aktuelle und nächste Jahr von der Feiertags-API.
     */
    @PostMapping("/import")
    public String importHolidays(RedirectAttributes redirectAttributes) {
        try {
            HolidayImportService.ImportResult result = holidayImportService.importHolidays();

            if (result.hasErrors()) {
                redirectAttributes.addFlashAttribute("warningMessage",
                    result.getSummary() + ". Es gab jedoch einige Fehler: " +
                    String.join(", ", result.errors()));
            } else {
                redirectAttributes.addFlashAttribute("successMessage", result.getSummary());
            }
        } catch (HolidayImportService.ImportException e) {
            log.error("Fehler beim Importieren der Feiertage", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                "Fehler beim Importieren der Feiertage: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unerwarteter Fehler beim Importieren der Feiertage", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                "Unerwarteter Fehler beim Importieren der Feiertage");
        }
        return "redirect:/holidays";
    }
}
