package name.saak.contactmanager.service;

import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.wml.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Service zur Konvertierung von Markdown-Dokumenten zu Word-Dokumenten (.docx).
 * Verwendet docx4j für die DOCX-Generierung.
 */
@Service
public class MarkdownToDocxService {

    private static final Logger log = LoggerFactory.getLogger(MarkdownToDocxService.class);

    private WordprocessingMLPackage wordMLPackage;
    private MainDocumentPart mainDocumentPart;
    private ObjectFactory factory;
    private int listLevel = 0;

    /**
     * Konvertiert eine Markdown-Datei zu einem Word-Dokument.
     *
     * @param markdownPath Pfad zur Markdown-Quelldatei
     * @param docxPath Pfad zur zu erstellenden Word-Datei
     * @throws Exception wenn Datei-Operationen fehlschlagen
     */
    public void convertMarkdownToDocx(Path markdownPath, Path docxPath) throws Exception {
        log.info("Converting Markdown to DOCX: {} -> {}", markdownPath, docxPath);

        // Markdown-Datei lesen
        String markdownContent = Files.readString(markdownPath);

        // Markdown parsen
        Parser parser = Parser.builder()
                .extensions(Arrays.asList(TablesExtension.create()))
                .build();
        Node document = parser.parse(markdownContent);

        // Word-Dokument erstellen
        convertToDocx(document, docxPath);

        log.info("Conversion completed successfully");
    }

    /**
     * Konvertiert einen geparsten Markdown-AST zu einem Word-Dokument.
     *
     * @param node Root-Node des Markdown-AST
     * @param outputPath Pfad zur zu erstellenden Word-Datei
     * @throws Docx4JException wenn das Dokument nicht erstellt werden kann
     */
    private void convertToDocx(Node node, Path outputPath) throws Docx4JException {
        wordMLPackage = WordprocessingMLPackage.createPackage();
        mainDocumentPart = wordMLPackage.getMainDocumentPart();
        factory = new ObjectFactory();
        listLevel = 0;

        // Dokument durchlaufen und konvertieren
        processNode(node);

        // Dokument speichern
        wordMLPackage.save(new File(outputPath.toString()));
    }

    /**
     * Verarbeitet einen Markdown-Node rekursiv.
     */
    private void processNode(Node node) {
        if (node instanceof org.commonmark.node.Document) {
            // Root-Node: alle Kinder verarbeiten
            for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
                processNode(child);
            }
        } else if (node instanceof Heading) {
            processHeading((Heading) node);
        } else if (node instanceof Paragraph) {
            processParagraph((Paragraph) node);
        } else if (node instanceof BulletList) {
            processBulletList((BulletList) node);
        } else if (node instanceof OrderedList) {
            processOrderedList((OrderedList) node);
        } else if (node instanceof org.commonmark.ext.gfm.tables.TableBlock) {
            processTable((org.commonmark.ext.gfm.tables.TableBlock) node);
        }
    }

    /**
     * Verarbeitet eine Überschrift.
     */
    private void processHeading(Heading heading) {
        P paragraph = factory.createP();
        PPr pPr = factory.createPPr();

        // Überschrift-Style setzen
        PPrBase.PStyle pStyle = factory.createPPrBasePStyle();
        pStyle.setVal("Heading" + heading.getLevel());
        pPr.setPStyle(pStyle);
        paragraph.setPPr(pPr);

        // Text extrahieren und hinzufügen
        extractAndAddText(heading, paragraph);

        mainDocumentPart.addObject(paragraph);
    }

    /**
     * Verarbeitet einen normalen Absatz.
     */
    private void processParagraph(Paragraph para) {
        P paragraph = factory.createP();

        // Inline-Elemente verarbeiten (Text, Bold, Italic, etc.)
        processInlineElements(para, paragraph);

        mainDocumentPart.addObject(paragraph);
    }

    /**
     * Verarbeitet Inline-Elemente wie Text, Fett, Kursiv.
     */
    private void processInlineElements(Node parent, P paragraph) {
        for (Node child = parent.getFirstChild(); child != null; child = child.getNext()) {
            if (child instanceof org.commonmark.node.Text) {
                R run = factory.createR();
                org.docx4j.wml.Text text = factory.createText();
                text.setValue(((org.commonmark.node.Text) child).getLiteral() + " ");
                run.getContent().add(text);
                paragraph.getContent().add(run);
            } else if (child instanceof StrongEmphasis) {
                R run = factory.createR();
                RPr rPr = factory.createRPr();
                BooleanDefaultTrue bold = factory.createBooleanDefaultTrue();
                rPr.setB(bold);
                run.setRPr(rPr);
                extractText(child, run);
                paragraph.getContent().add(run);
            } else if (child instanceof Emphasis) {
                R run = factory.createR();
                RPr rPr = factory.createRPr();
                BooleanDefaultTrue italic = factory.createBooleanDefaultTrue();
                rPr.setI(italic);
                run.setRPr(rPr);
                extractText(child, run);
                paragraph.getContent().add(run);
            } else if (child instanceof SoftLineBreak) {
                R run = factory.createR();
                org.docx4j.wml.Text text = factory.createText();
                text.setValue(" ");
                run.getContent().add(text);
                paragraph.getContent().add(run);
            } else if (child instanceof HardLineBreak) {
                R run = factory.createR();
                Br br = factory.createBr();
                run.getContent().add(br);
                paragraph.getContent().add(run);
            } else {
                // Rekursiv verarbeiten
                processInlineElements(child, paragraph);
            }
        }
    }

    /**
     * Extrahiert Text aus einem Node und fügt ihn zu einem Run hinzu.
     */
    private void extractText(Node node, R run) {
        for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
            if (child instanceof org.commonmark.node.Text) {
                org.docx4j.wml.Text text = factory.createText();
                text.setValue(((org.commonmark.node.Text) child).getLiteral() + " ");
                run.getContent().add(text);
            } else {
                extractText(child, run);
            }
        }
    }

    /**
     * Extrahiert Text aus einem Node und fügt ihn zu einem Paragraph hinzu.
     */
    private void extractAndAddText(Node node, P paragraph) {
        for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
            if (child instanceof org.commonmark.node.Text) {
                R run = factory.createR();
                org.docx4j.wml.Text text = factory.createText();
                text.setValue(((org.commonmark.node.Text) child).getLiteral());
                run.getContent().add(text);
                paragraph.getContent().add(run);
            } else if (child instanceof StrongEmphasis) {
                R run = factory.createR();
                RPr rPr = factory.createRPr();
                BooleanDefaultTrue bold = factory.createBooleanDefaultTrue();
                rPr.setB(bold);
                run.setRPr(rPr);
                extractText(child, run);
                paragraph.getContent().add(run);
            } else if (child instanceof Emphasis) {
                R run = factory.createR();
                RPr rPr = factory.createRPr();
                BooleanDefaultTrue italic = factory.createBooleanDefaultTrue();
                rPr.setI(italic);
                run.setRPr(rPr);
                extractText(child, run);
                paragraph.getContent().add(run);
            } else {
                extractAndAddText(child, paragraph);
            }
        }
    }

    /**
     * Verarbeitet eine Bullet-Liste.
     */
    private void processBulletList(BulletList list) {
        listLevel++;
        for (Node child = list.getFirstChild(); child != null; child = child.getNext()) {
            if (child instanceof ListItem) {
                processListItem((ListItem) child, false);
            }
        }
        listLevel--;
    }

    /**
     * Verarbeitet eine nummerierte Liste.
     */
    private void processOrderedList(OrderedList list) {
        listLevel++;
        for (Node child = list.getFirstChild(); child != null; child = child.getNext()) {
            if (child instanceof ListItem) {
                processListItem((ListItem) child, true);
            }
        }
        listLevel--;
    }

    /**
     * Verarbeitet ein Listen-Element.
     */
    private void processListItem(ListItem item, boolean ordered) {
        P paragraph = factory.createP();
        PPr pPr = factory.createPPr();

        // Nummerierung hinzufügen
        PPrBase.NumPr numPr = factory.createPPrBaseNumPr();
        PPrBase.NumPr.Ilvl ilvl = factory.createPPrBaseNumPrIlvl();
        ilvl.setVal(BigInteger.valueOf(listLevel - 1));
        numPr.setIlvl(ilvl);

        PPrBase.NumPr.NumId numId = factory.createPPrBaseNumPrNumId();
        numId.setVal(BigInteger.valueOf(ordered ? 1 : 2));
        numPr.setNumId(numId);

        pPr.setNumPr(numPr);
        paragraph.setPPr(pPr);

        // Listeninhalt verarbeiten
        for (Node child = item.getFirstChild(); child != null; child = child.getNext()) {
            if (child instanceof Paragraph) {
                processInlineElements(child, paragraph);
            } else if (child instanceof BulletList) {
                mainDocumentPart.addObject(paragraph);
                processBulletList((BulletList) child);
                return;
            } else if (child instanceof OrderedList) {
                mainDocumentPart.addObject(paragraph);
                processOrderedList((OrderedList) child);
                return;
            }
        }

        mainDocumentPart.addObject(paragraph);
    }

    /**
     * Verarbeitet eine Tabelle.
     */
    private void processTable(org.commonmark.ext.gfm.tables.TableBlock tableBlock) {
        Tbl table = factory.createTbl();

        // Tabellen-Eigenschaften
        TblPr tblPr = factory.createTblPr();
        TblWidth tblWidth = factory.createTblWidth();
        tblWidth.setW(BigInteger.valueOf(5000));
        tblWidth.setType("pct");
        tblPr.setTblW(tblWidth);
        table.setTblPr(tblPr);

        // Tabelle befüllen
        for (Node child = tableBlock.getFirstChild(); child != null; child = child.getNext()) {
            if (child instanceof org.commonmark.ext.gfm.tables.TableHead) {
                processTableSection((org.commonmark.ext.gfm.tables.TableHead) child, table, true);
            } else if (child instanceof org.commonmark.ext.gfm.tables.TableBody) {
                processTableSection((org.commonmark.ext.gfm.tables.TableBody) child, table, false);
            }
        }

        mainDocumentPart.addObject(table);
    }

    /**
     * Verarbeitet einen Tabellenabschnitt (Head oder Body).
     */
    private void processTableSection(Node section, Tbl table, boolean isHeader) {
        for (Node row = section.getFirstChild(); row != null; row = row.getNext()) {
            if (row instanceof org.commonmark.ext.gfm.tables.TableRow) {
                Tr tableRow = factory.createTr();

                for (Node cell = row.getFirstChild(); cell != null; cell = cell.getNext()) {
                    if (cell instanceof org.commonmark.ext.gfm.tables.TableCell) {
                        Tc tableCell = factory.createTc();

                        // Zellinhalt extrahieren
                        StringBuilder cellText = new StringBuilder();
                        extractTableCellText(cell, cellText);

                        P paragraph = factory.createP();
                        R run = factory.createR();

                        if (isHeader) {
                            RPr rPr = factory.createRPr();
                            BooleanDefaultTrue bold = factory.createBooleanDefaultTrue();
                            rPr.setB(bold);
                            run.setRPr(rPr);
                        }

                        org.docx4j.wml.Text text = factory.createText();
                        text.setValue(cellText.toString());
                        run.getContent().add(text);
                        paragraph.getContent().add(run);
                        tableCell.getContent().add(paragraph);
                        tableRow.getContent().add(tableCell);
                    }
                }
                table.getContent().add(tableRow);
            }
        }
    }

    /**
     * Extrahiert Text aus einer Tabellenzelle.
     */
    private void extractTableCellText(Node node, StringBuilder text) {
        for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
            if (child instanceof org.commonmark.node.Text) {
                text.append(((org.commonmark.node.Text) child).getLiteral());
            } else {
                extractTableCellText(child, text);
            }
        }
    }
}
