package name.saak.contactmanager.service;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class MarkdownToDocxServiceTest {

    @Autowired
    private MarkdownToDocxService markdownToDocxService;

    @Test
    @DisplayName("Should convert Quelldokument.md to Ergebnisdokument.docx")
    void testConvertQuelldokumentToErgebnisdokument() throws Exception {
        // Given: Pfade zu den Test-Dateien
        Path markdownPath = Paths.get("src/test/resources/Quelldokument.md");
        Path docxPath = Paths.get("src/test/resources/Ergebnisdokument.docx");

        // Alte Ergebnisdatei löschen falls vorhanden
        Files.deleteIfExists(docxPath);

        // When: Konvertierung durchführen
        markdownToDocxService.convertMarkdownToDocx(markdownPath, docxPath);

        // Then: Prüfen dass Datei erstellt wurde
        assertThat(Files.exists(docxPath)).isTrue();
        assertThat(Files.size(docxPath)).isGreaterThan(0);

        // Dokument öffnen und Inhalt prüfen
        try (FileInputStream fis = new FileInputStream(docxPath.toFile());
             XWPFDocument document = new XWPFDocument(fis)) {

            List<XWPFParagraph> paragraphs = document.getParagraphs();
            List<XWPFTable> tables = document.getTables();

            // Mindestens einige Absätze vorhanden
            assertThat(paragraphs).isNotEmpty();

            // Überschriften vorhanden
            boolean hasHeading1 = paragraphs.stream()
                    .anyMatch(p -> "Heading1".equals(p.getStyle()));
            boolean hasHeading2 = paragraphs.stream()
                    .anyMatch(p -> "Heading2".equals(p.getStyle()));

            assertThat(hasHeading1).isTrue();
            assertThat(hasHeading2).isTrue();

            // Tabelle vorhanden
            assertThat(tables).isNotEmpty();
            assertThat(tables.get(0).getNumberOfRows()).isGreaterThan(0);

            // Text mit Formatierung vorhanden
            boolean hasBoldText = paragraphs.stream()
                    .anyMatch(p -> p.getRuns().stream()
                            .anyMatch(r -> r.isBold()));
            boolean hasItalicText = paragraphs.stream()
                    .anyMatch(p -> p.getRuns().stream()
                            .anyMatch(r -> r.isItalic()));

            assertThat(hasBoldText).isTrue();
            assertThat(hasItalicText).isTrue();
        }
    }

    @Test
    @DisplayName("Should handle headings correctly")
    void testHeadingConversion() throws Exception {
        // Given: Markdown mit verschiedenen Überschriften
        Path markdownPath = Files.createTempFile("test", ".md");
        Path docxPath = Files.createTempFile("test", ".docx");

        String markdown = """
                # Hauptüberschrift

                ## Unterüberschrift

                Normaler Text
                """;

        Files.writeString(markdownPath, markdown);

        try {
            // When
            markdownToDocxService.convertMarkdownToDocx(markdownPath, docxPath);

            // Then
            assertThat(Files.exists(docxPath)).isTrue();

            try (FileInputStream fis = new FileInputStream(docxPath.toFile());
                 XWPFDocument document = new XWPFDocument(fis)) {

                List<XWPFParagraph> paragraphs = document.getParagraphs();

                // Überschriften haben korrekte Stile
                assertThat(paragraphs).hasSizeGreaterThanOrEqualTo(3);
                assertThat(paragraphs.get(0).getStyle()).isEqualTo("Heading1");
                assertThat(paragraphs.get(1).getStyle()).isEqualTo("Heading2");
            }
        } finally {
            Files.deleteIfExists(markdownPath);
            Files.deleteIfExists(docxPath);
        }
    }

    @Test
    @DisplayName("Should handle lists correctly")
    void testListConversion() throws Exception {
        // Given: Markdown mit Listen
        Path markdownPath = Files.createTempFile("test", ".md");
        Path docxPath = Files.createTempFile("test", ".docx");

        String markdown = """
                - Listenpunkt 1
                - Listenpunkt 2
                  - Verschachtelt

                1. Nummeriert 1
                2. Nummeriert 2
                """;

        Files.writeString(markdownPath, markdown);

        try {
            // When
            markdownToDocxService.convertMarkdownToDocx(markdownPath, docxPath);

            // Then
            assertThat(Files.exists(docxPath)).isTrue();

            try (FileInputStream fis = new FileInputStream(docxPath.toFile());
                 XWPFDocument document = new XWPFDocument(fis)) {

                List<XWPFParagraph> paragraphs = document.getParagraphs();

                // Listen wurden erstellt
                assertThat(paragraphs).isNotEmpty();

                // Mindestens ein Absatz hat Nummerierung
                boolean hasNumbering = paragraphs.stream()
                        .anyMatch(p -> p.getNumID() != null && p.getNumID().intValue() > 0);

                assertThat(hasNumbering).isTrue();
            }
        } finally {
            Files.deleteIfExists(markdownPath);
            Files.deleteIfExists(docxPath);
        }
    }

    @Test
    @DisplayName("Should handle text formatting correctly")
    void testTextFormatting() throws Exception {
        // Given: Markdown mit Formatierung
        Path markdownPath = Files.createTempFile("test", ".md");
        Path docxPath = Files.createTempFile("test", ".docx");

        String markdown = """
                **Fetter Text** und *kursiver Text*
                """;

        Files.writeString(markdownPath, markdown);

        try {
            // When
            markdownToDocxService.convertMarkdownToDocx(markdownPath, docxPath);

            // Then
            assertThat(Files.exists(docxPath)).isTrue();

            try (FileInputStream fis = new FileInputStream(docxPath.toFile());
                 XWPFDocument document = new XWPFDocument(fis)) {

                List<XWPFParagraph> paragraphs = document.getParagraphs();
                assertThat(paragraphs).isNotEmpty();

                XWPFParagraph para = paragraphs.get(0);

                // Fetter und kursiver Text vorhanden
                boolean hasBold = para.getRuns().stream().anyMatch(r -> r.isBold());
                boolean hasItalic = para.getRuns().stream().anyMatch(r -> r.isItalic());

                assertThat(hasBold).isTrue();
                assertThat(hasItalic).isTrue();
            }
        } finally {
            Files.deleteIfExists(markdownPath);
            Files.deleteIfExists(docxPath);
        }
    }

    @Test
    @DisplayName("Should handle tables correctly")
    void testTableConversion() throws Exception {
        // Given: Markdown mit Tabelle
        Path markdownPath = Files.createTempFile("test", ".md");
        Path docxPath = Files.createTempFile("test", ".docx");

        String markdown = """
                | Spalte 1 | Spalte 2 |
                | --- | --- |
                | Wert 1 | Wert 2 |
                | Wert 3 | Wert 4 |
                """;

        Files.writeString(markdownPath, markdown);

        try {
            // When
            markdownToDocxService.convertMarkdownToDocx(markdownPath, docxPath);

            // Then
            assertThat(Files.exists(docxPath)).isTrue();

            try (FileInputStream fis = new FileInputStream(docxPath.toFile());
                 XWPFDocument document = new XWPFDocument(fis)) {

                List<XWPFTable> tables = document.getTables();

                // Tabelle wurde erstellt
                assertThat(tables).hasSize(1);

                XWPFTable table = tables.get(0);
                assertThat(table.getNumberOfRows()).isEqualTo(3);  // Header + 2 Datenzeilen

                // Header ist fett
                boolean headerIsBold = table.getRow(0).getTableCells().stream()
                        .anyMatch(cell -> cell.getParagraphs().stream()
                                .anyMatch(p -> p.getRuns().stream()
                                        .anyMatch(r -> r.isBold())));

                assertThat(headerIsBold).isTrue();
            }
        } finally {
            Files.deleteIfExists(markdownPath);
            Files.deleteIfExists(docxPath);
        }
    }
}
