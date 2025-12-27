package name.saak.contactmanager.service;

import name.saak.contactmanager.domain.Contact;
import name.saak.contactmanager.repository.ContactRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ExcelExportServiceTest {

    @Autowired
    private ExcelExportService excelExportService;

    @Autowired
    private ContactRepository contactRepository;

    @Test
    void shouldExportContactsToExcel() throws IOException {
        // Given
        List<Contact> contacts = contactRepository.findAllByOrderByNachnameAscVornameAsc();

        // When
        byte[] excelData = excelExportService.exportContactsToExcel(contacts);

        // Then
        assertThat(excelData).isNotEmpty();

        // Verify Excel structure
        try (InputStream inputStream = new ByteArrayInputStream(excelData);
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheet("Kontakte");
            assertThat(sheet).isNotNull();

            // Verify header row
            Row headerRow = sheet.getRow(0);
            assertThat(headerRow.getCell(0).getStringCellValue()).isEqualTo("Firma");
            assertThat(headerRow.getCell(1).getStringCellValue()).isEqualTo("Anrede");
            assertThat(headerRow.getCell(2).getStringCellValue()).isEqualTo("Vorname_Name");
            assertThat(headerRow.getCell(3).getStringCellValue()).isEqualTo("Straße");
            assertThat(headerRow.getCell(4).getStringCellValue()).isEqualTo("PLZ/Ort");

            // Verify we have data rows (3 test contacts + header)
            assertThat(sheet.getPhysicalNumberOfRows()).isEqualTo(contacts.size() + 1);
        }
    }

    @Test
    void shouldMatchExpectedExcelFormat() throws IOException {
        // Given
        List<Contact> contacts = contactRepository.findAllByOrderByNachnameAscVornameAsc();

        // When
        byte[] actualExcelData = excelExportService.exportContactsToExcel(contacts);

        // Then - Verify against expected Excel file
        String expectedFilePath = "src/test/resources/Export.xlsx";

        try (InputStream actualStream = new ByteArrayInputStream(actualExcelData);
             Workbook actualWorkbook = new XSSFWorkbook(actualStream)) {

            Sheet actualSheet = actualWorkbook.getSheet("Kontakte");
            assertThat(actualSheet).isNotNull();

            // Verify data rows match expected format
            // Row 1: Erika Musterfrau (alphabetically first by nachname)
            Row row1 = actualSheet.getRow(1);
            assertThat(getCellValueAsString(row1.getCell(0))).isEqualTo(""); // Firma
            assertThat(getCellValueAsString(row1.getCell(1))).isEqualTo("Frau"); // Anrede
            assertThat(getCellValueAsString(row1.getCell(2))).isEqualTo("Erika Musterfrau"); // Vorname_Name
            assertThat(getCellValueAsString(row1.getCell(3))).isEqualTo("Nebenstraße 5"); // Straße
            assertThat(getCellValueAsString(row1.getCell(4))).isEqualTo("54321 München"); // PLZ/Ort

            // Row 2: Max Mustermann
            Row row2 = actualSheet.getRow(2);
            assertThat(getCellValueAsString(row2.getCell(0))).isEqualTo(""); // Firma
            assertThat(getCellValueAsString(row2.getCell(1))).isEqualTo("Herr"); // Anrede
            assertThat(getCellValueAsString(row2.getCell(2))).isEqualTo("Max Mustermann"); // Vorname_Name
            assertThat(getCellValueAsString(row2.getCell(3))).isEqualTo("Hauptstraße 1"); // Straße
            assertThat(getCellValueAsString(row2.getCell(4))).isEqualTo("12345 Berlin"); // PLZ/Ort

            // Row 3: Hans Schmidt
            Row row3 = actualSheet.getRow(3);
            assertThat(getCellValueAsString(row3.getCell(0))).isEqualTo(""); // Firma
            assertThat(getCellValueAsString(row3.getCell(1))).isEqualTo(""); // Anrede (null)
            assertThat(getCellValueAsString(row3.getCell(2))).isEqualTo("Hans Schmidt"); // Vorname_Name
            assertThat(getCellValueAsString(row3.getCell(3))).isEqualTo("Dorfstraße 10"); // Straße
            assertThat(getCellValueAsString(row3.getCell(4))).isEqualTo("67890 Hamburg"); // PLZ/Ort
        }
    }

    @Test
    void shouldExportEmptyListSuccessfully() throws IOException {
        // Given
        List<Contact> emptyList = List.of();

        // When
        byte[] excelData = excelExportService.exportContactsToExcel(emptyList);

        // Then
        assertThat(excelData).isNotEmpty();

        try (InputStream inputStream = new ByteArrayInputStream(excelData);
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheet("Kontakte");
            assertThat(sheet).isNotNull();
            assertThat(sheet.getPhysicalNumberOfRows()).isEqualTo(1); // Only header row
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        return cell.getStringCellValue();
    }
}
