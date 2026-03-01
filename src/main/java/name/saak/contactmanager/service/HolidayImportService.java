package name.saak.contactmanager.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import name.saak.contactmanager.domain.Holiday;
import name.saak.contactmanager.dto.HolidayApiResponse;
import name.saak.contactmanager.repository.HolidayRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class HolidayImportService {

    private static final Logger log = LoggerFactory.getLogger(HolidayImportService.class);
    private static final String API_URL = "https://feiertage-api.de/api/?jahr={year}&nur_land={region}";
    private static final int MAX_NAME_LENGTH = 50;
    private static final int MAX_DESCRIPTION_LENGTH = 500;

    private final HolidayRepository holidayRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.holidays.region:BY}")
    private String region;

    public HolidayImportService(HolidayRepository holidayRepository,
                                RestTemplate restTemplate,
                                ObjectMapper objectMapper) {
        this.holidayRepository = holidayRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Importiert Feiertage für das aktuelle und nächste Jahr.
     * @return Anzahl der importierten Feiertage
     */
    public ImportResult importHolidays() {
        int currentYear = Year.now().getValue();
        int nextYear = currentYear + 1;

        log.info("=== Starte Feiertags-Import ===");
        log.info("Region: {}", region);
        log.info("Jahre: {} und {}", currentYear, nextYear);

        int importedCount = 0;
        int skippedCount = 0;
        List<String> errors = new ArrayList<>();

        // Importiere Feiertage für aktuelles Jahr
        log.info("--- Importiere Feiertage für Jahr {} ---", currentYear);
        try {
            ImportResult currentYearResult = importHolidaysForYear(currentYear);
            importedCount += currentYearResult.imported();
            skippedCount += currentYearResult.skipped();
            errors.addAll(currentYearResult.errors());
            log.info("Jahr {}: {} importiert, {} übersprungen",
                currentYear, currentYearResult.imported(), currentYearResult.skipped());
        } catch (Exception e) {
            log.error("Fehler beim Import für Jahr {}", currentYear, e);
            errors.add("Fehler beim Import für " + currentYear + ": " + e.getMessage());
        }

        // Importiere Feiertage für nächstes Jahr
        log.info("--- Importiere Feiertage für Jahr {} ---", nextYear);
        try {
            ImportResult nextYearResult = importHolidaysForYear(nextYear);
            importedCount += nextYearResult.imported();
            skippedCount += nextYearResult.skipped();
            errors.addAll(nextYearResult.errors());
            log.info("Jahr {}: {} importiert, {} übersprungen",
                nextYear, nextYearResult.imported(), nextYearResult.skipped());
        } catch (Exception e) {
            log.error("Fehler beim Import für Jahr {}", nextYear, e);
            errors.add("Fehler beim Import für " + nextYear + ": " + e.getMessage());
        }

        log.info("=== Feiertags-Import abgeschlossen ===");
        log.info("Gesamt: {} importiert, {} übersprungen, {} Fehler",
            importedCount, skippedCount, errors.size());
        if (!errors.isEmpty()) {
            log.warn("Fehler beim Import: {}", errors);
        }

        return new ImportResult(importedCount, skippedCount, errors);
    }

    /**
     * Importiert Feiertage für ein bestimmtes Jahr.
     */
    private ImportResult importHolidaysForYear(int year) throws Exception {
        String url = API_URL.replace("{year}", String.valueOf(year)).replace("{region}", region);
        log.info("API-Aufruf: {}", url);

        String response;
        try {
            response = restTemplate.getForObject(url, String.class);
            log.debug("API-Antwort erhalten: {} Zeichen", response != null ? response.length() : 0);
        } catch (Exception e) {
            log.error("Fehler beim API-Aufruf: {}", e.getMessage());
            throw new ImportException("API-Aufruf fehlgeschlagen: " + e.getMessage(), e);
        }

        if (response == null || response.trim().isEmpty()) {
            log.error("API hat keine Daten zurückgegeben");
            throw new ImportException("API hat keine Daten für Jahr " + year + " zurückgegeben");
        }

        // Parse JSON response
        Map<String, HolidayApiResponse> holidaysMap;
        try {
            holidaysMap = objectMapper.readValue(
                response,
                new TypeReference<Map<String, HolidayApiResponse>>() {}
            );
            log.info("API-Antwort geparst: {} Feiertage gefunden", holidaysMap.size());
        } catch (Exception e) {
            log.error("Fehler beim Parsen der API-Antwort: {}", e.getMessage());
            log.debug("API-Antwort: {}", response);
            throw new ImportException("Fehler beim Parsen der API-Antwort: " + e.getMessage(), e);
        }

        int imported = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        for (Map.Entry<String, HolidayApiResponse> entry : holidaysMap.entrySet()) {
            String name = entry.getKey();
            HolidayApiResponse apiResponse = entry.getValue();

            try {
                LocalDate date = LocalDate.parse(apiResponse.getDatum());
                log.debug("Verarbeite: {} am {}", name, date);

                // Prüfen, ob Feiertag bereits existiert
                if (holidayRepository.existsById(date)) {
                    log.info("  -> ÜBERSPRUNGEN: {} am {} existiert bereits", name, date);
                    skipped++;
                    continue;
                }

                // Name-Länge validieren und ggf. kürzen
                String validatedName = name;
                if (name.length() > MAX_NAME_LENGTH) {
                    validatedName = name.substring(0, MAX_NAME_LENGTH);
                    log.warn("  -> Name zu lang ({} Zeichen), gekürzt auf {} Zeichen: {} -> {}",
                        name.length(), MAX_NAME_LENGTH, name, validatedName);
                }

                // Beschreibung validieren und ggf. kürzen
                String beschreibung = null;
                if (apiResponse.getHinweis() != null && !apiResponse.getHinweis().trim().isEmpty()) {
                    String hinweis = apiResponse.getHinweis().trim();
                    if (hinweis.length() > MAX_DESCRIPTION_LENGTH) {
                        beschreibung = hinweis.substring(0, MAX_DESCRIPTION_LENGTH);
                        log.warn("  -> Beschreibung zu lang ({} Zeichen), gekürzt auf {} Zeichen",
                            hinweis.length(), MAX_DESCRIPTION_LENGTH);
                    } else {
                        beschreibung = hinweis;
                    }
                }

                // Neuen Feiertag erstellen
                Holiday holiday = new Holiday();
                holiday.setDate(date);
                holiday.setName(validatedName);
                holiday.setFrei(true); // Alle Feiertage sind arbeitsfrei
                holiday.setBeschreibung(beschreibung);

                holidayRepository.save(holiday);
                log.info("  -> IMPORTIERT: {} am {} (frei=true{})",
                    validatedName, date, beschreibung != null ? ", mit Beschreibung" : "");
                imported++;

            } catch (Exception e) {
                log.error("  -> FEHLER bei '{}': {}", name, e.getMessage(), e);
                errors.add("Fehler bei '" + name + "': " + e.getMessage());
            }
        }

        log.info("Import für Jahr {} abgeschlossen: {} importiert, {} übersprungen, {} Fehler",
            year, imported, skipped, errors.size());
        return new ImportResult(imported, skipped, errors);
    }

    /**
     * Ergebnis des Imports.
     */
    public record ImportResult(int imported, int skipped, List<String> errors) {
        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public String getSummary() {
            return String.format("%d Feiertag(e) importiert, %d übersprungen", imported, skipped);
        }
    }

    /**
     * Exception für Import-Fehler.
     */
    public static class ImportException extends RuntimeException {
        public ImportException(String message) {
            super(message);
        }

        public ImportException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
