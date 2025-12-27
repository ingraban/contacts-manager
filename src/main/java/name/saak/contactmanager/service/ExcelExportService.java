package name.saak.contactmanager.service;

import name.saak.contactmanager.domain.Contact;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class ExcelExportService {

    /**
     * Exportiert eine Liste von Kontakten als Excel-Datei (XLSX).
     *
     * @param contacts Liste der zu exportierenden Kontakte
     * @return Byte-Array der Excel-Datei
     * @throws IOException bei Fehlern beim Erstellen der Excel-Datei
     */
    public byte[] exportContactsToExcel(List<Contact> contacts) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Kontakte");

            // Header-Zeile erstellen
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = createHeaderStyle(workbook);

            String[] headers = {"Firma", "Anrede", "Vorname_Name", "Straße", "PLZ/Ort"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Datenzeilen erstellen
            int rowNum = 1;
            for (Contact contact : contacts) {
                Row row = sheet.createRow(rowNum++);

                // Firma
                row.createCell(0).setCellValue(contact.getFirma() != null ? contact.getFirma() : "");

                // Anrede
                row.createCell(1).setCellValue(contact.getAnrede() != null ? contact.getAnrede() : "");

                // Vorname_Name
                String vornameName = buildVornameName(contact);
                row.createCell(2).setCellValue(vornameName);

                // Straße
                row.createCell(3).setCellValue(contact.getStrasse() != null ? contact.getStrasse() : "");

                // PLZ/Ort
                String plzOrt = buildPlzOrt(contact);
                row.createCell(4).setCellValue(plzOrt);
            }

            // Spaltenbreite automatisch anpassen
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * Erstellt den Stil für die Header-Zeile.
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * Baut den Vorname_Name String.
     * Format: "Vorname Nachname" wenn Vorname existiert, sonst nur "Nachname"
     */
    private String buildVornameName(Contact contact) {
        if (contact.getVorname() != null && !contact.getVorname().isEmpty()) {
            return contact.getVorname() + " " + contact.getNachname();
        }
        return contact.getNachname();
    }

    /**
     * Baut den PLZ/Ort String.
     * Format: "PLZ Ort" wenn PLZ existiert, sonst nur "Ort"
     */
    private String buildPlzOrt(Contact contact) {
        if (contact.getPostleitzahl() != null && !contact.getPostleitzahl().isEmpty()) {
            return contact.getPostleitzahl() + " " + contact.getOrt();
        }
        return contact.getOrt();
    }
}
