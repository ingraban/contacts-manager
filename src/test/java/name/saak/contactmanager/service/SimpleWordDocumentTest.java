package name.saak.contactmanager.service;

import java.io.FileInputStream;
import org.apache.poi.xwpf.usermodel.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class SimpleWordDocumentTest {

    @Test
    @Disabled("Experimental test - not part of main functionality")
    void createSimpleDocument() throws Exception {
        // Template laden (mit Styles)
        try (FileInputStream templateInput = new FileInputStream(
                "/Users/saak/git/Privat/contacts-manager/src/test/resources/template.docx");
                XWPFDocument document = new XWPFDocument(templateInput)) {
            // Alle existierenden Absätze löschen (außer erstem)
            while (document.getParagraphs().size() > 1) {


                document.removeBodyElement(1);
            }
   document.getStyles().getUsedStyleList(null).forEach(s -> {
        System.out.println("Style: " + s.getStyleId() 
            + " | Name: " + s.getName()
            + " | Type: " + s.getType());
    });
            
            // Neuen Heading1-Absatz hinzufügen
            XWPFParagraph heading = document.getParagraphs().get(0);
            heading.setStyle("Heading1");
            heading.createRun().setText("Überschrift");
            
            // Neuen Normal-Absatz hinzufügen
            XWPFParagraph normal = document.createParagraph();
            normal.setStyle("Normal");
            normal.createRun().setText("Dies ist der Absatz.");
            
            // Speichern
            document.write(new java.io.FileOutputStream("/Users/saak/git/Privat/contacts-manager/src/test/resources/test.docx"));
            document.close();
        }
    }
}
