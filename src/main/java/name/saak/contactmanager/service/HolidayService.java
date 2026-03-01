package name.saak.contactmanager.service;

import name.saak.contactmanager.domain.Holiday;
import name.saak.contactmanager.repository.HolidayRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class HolidayService {

    private final HolidayRepository holidayRepository;

    public HolidayService(HolidayRepository holidayRepository) {
        this.holidayRepository = holidayRepository;
    }

    /**
     * Gibt alle Feiertage sortiert zurück (neueste zuerst).
     */
    @Transactional(readOnly = true)
    public List<Holiday> findAllHolidays() {
        return holidayRepository.findAllByOrderByDateDesc();
    }

    /**
     * Sucht einen Feiertag anhand des Datums.
     */
    @Transactional(readOnly = true)
    public Optional<Holiday> findHolidayByDate(LocalDate date) {
        return holidayRepository.findById(date);
    }

    /**
     * Findet Feiertage eines bestimmten Jahres.
     */
    @Transactional(readOnly = true)
    public List<Holiday> findHolidaysByYear(int year) {
        return holidayRepository.findByYear(year);
    }

    /**
     * Sucht Feiertage anhand eines Suchbegriffs.
     * Gibt alle Feiertage zurück wenn der Suchbegriff leer ist.
     */
    @Transactional(readOnly = true)
    public List<Holiday> searchHolidays(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return findAllHolidays();
        }
        return holidayRepository.searchHolidays(searchTerm.trim());
    }

    /**
     * Speichert einen neuen Feiertag.
     *
     * @throws DuplicateHolidayException wenn am gleichen Datum bereits ein Feiertag existiert
     */
    public Holiday createHoliday(Holiday holiday) {
        validateUniqueDate(holiday.getDate(), null);
        normalizeEmptyFields(holiday);
        return holidayRepository.save(holiday);
    }

    /**
     * Aktualisiert einen bestehenden Feiertag.
     *
     * @throws HolidayNotFoundException wenn der Feiertag nicht gefunden wird
     * @throws DuplicateHolidayException wenn am gleichen Datum bereits ein anderer Feiertag existiert
     */
    public Holiday updateHoliday(LocalDate originalDate, Holiday updatedHoliday) {
        Holiday existing = holidayRepository.findById(originalDate)
            .orElseThrow(() -> new HolidayNotFoundException("Feiertag nicht gefunden"));

        // Wenn das Datum geändert wurde, prüfen ob es bereits existiert
        if (!existing.getDate().equals(updatedHoliday.getDate())) {
            validateUniqueDate(updatedHoliday.getDate(), originalDate);
            // Alten Eintrag löschen, neuen erstellen (da Datum der Primary Key ist)
            holidayRepository.delete(existing);
            normalizeEmptyFields(updatedHoliday);
            return holidayRepository.save(updatedHoliday);
        } else {
            // Datum unverändert, nur Felder aktualisieren
            existing.setName(updatedHoliday.getName());
            existing.setBeschreibung(updatedHoliday.getBeschreibung());
            existing.setFrei(updatedHoliday.getFrei());
            normalizeEmptyFields(existing);
            return holidayRepository.save(existing);
        }
    }

    /**
     * Löscht einen Feiertag.
     *
     * @throws HolidayNotFoundException wenn der Feiertag nicht gefunden wird
     */
    public void deleteHoliday(LocalDate date) {
        if (!holidayRepository.existsById(date)) {
            throw new HolidayNotFoundException("Feiertag nicht gefunden");
        }
        holidayRepository.deleteById(date);
    }

    /**
     * Normalisiert leere Felder zu null.
     */
    private void normalizeEmptyFields(Holiday holiday) {
        if (holiday.getBeschreibung() != null && holiday.getBeschreibung().trim().isEmpty()) {
            holiday.setBeschreibung(null);
        }
    }

    /**
     * Validiert dass das Datum eindeutig ist.
     *
     * @param date Datum des Feiertags
     * @param excludeDate Datum das ausgeschlossen werden soll (bei Updates)
     * @throws DuplicateHolidayException wenn am Datum bereits ein Feiertag existiert
     */
    private void validateUniqueDate(LocalDate date, LocalDate excludeDate) {
        if (date == null) {
            return;
        }

        // Bei Updates: existierendes Datum ausschließen
        if (excludeDate != null) {
            if (holidayRepository.existsByDateAndDateNot(date, excludeDate)) {
                throw new DuplicateHolidayException(
                    "Am " + date + " existiert bereits ein Feiertag"
                );
            }
        } else {
            // Bei neuen Feiertagen: prüfen ob Datum schon existiert
            if (holidayRepository.existsById(date)) {
                throw new DuplicateHolidayException(
                    "Am " + date + " existiert bereits ein Feiertag"
                );
            }
        }
    }

    /**
     * Exception für nicht gefundene Feiertage.
     */
    public static class HolidayNotFoundException extends RuntimeException {
        private static final long serialVersionUID = 202603011500L;

        public HolidayNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Exception für doppelte Feiertage.
     */
    public static class DuplicateHolidayException extends RuntimeException {
        private static final long serialVersionUID = 202603011501L;

        public DuplicateHolidayException(String message) {
            super(message);
        }
    }
}
