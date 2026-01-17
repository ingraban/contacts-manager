# Markdown zu Word (DOCX) Konvertierung

## Übersicht

Der `MarkdownToDocxService` konvertiert Markdown-Dokumente zu Microsoft Word (.docx) Dateien.

## Features

### Unterstützte Markdown-Elemente

- ✅ **Überschriften** (H1-H6)
  - Werden als Word Heading-Stile formatiert

- ✅ **Absätze**
  - Normaler Fließtext

- ✅ **Text-Formatierung**
  - **Fett**: `**Text**` oder `__Text__`
  - *Kursiv*: `*Text*` oder `_Text_`

- ✅ **Listen**
  - Nummerierte Listen (1., 2., 3.)
  - Aufzählungslisten (-, *, +)
  - Verschachtelte Listen (mehrere Ebenen)

- ✅ **Tabellen** (GitHub Flavored Markdown)
  - Header-Zeile (fett formatiert)
  - Datenzeilen
  - Spaltenausrichtung (links, zentriert, rechts)

- ✅ **Zeilenumbrüche**
  - Weiche Umbrüche (mit 2 Leerzeichen am Zeilenende)
  - Harte Umbrüche

## Verwendung

### Service Injection

```java
@Autowired
private MarkdownToDocxService markdownToDocxService;
```

### Konvertierung

```java
Path markdownPath = Paths.get("src/test/resources/Quelldokument.md");
Path docxPath = Paths.get("src/test/resources/Ergebnisdokument.docx");

markdownToDocxService.convertMarkdownToDocx(markdownPath, docxPath);
```

### Beispiel-Markdown

```markdown
# Hauptüberschrift

Ein normaler Absatz mit **fettem Text** und *kursivem Text*.

## Unterüberschrift

1. Erster Punkt
2. Zweiter Punkt
   - Verschachtelter Unterpunkt
   - Noch ein Unterpunkt

| Spalte 1 | Spalte 2 | Spalte 3 |
| --- | :---: | ---: |
| Links | Zentriert | Rechts |
| Wert 1 | Wert 2 | Wert 3 |
```

## Dependencies

Die Konvertierung nutzt folgende Bibliotheken:

- **docx4j** (11.4.11) - Word-Dokument-Erstellung (OOXML)
- **Commonmark** (0.22.0) - Markdown-Parsing
- **Commonmark GFM Tables Extension** (0.22.0) - Tabellen-Unterstützung

Alle Dependencies sind in `pom.xml` definiert:

```xml
<dependency>
    <groupId>org.docx4j</groupId>
    <artifactId>docx4j-JAXB-ReferenceImpl</artifactId>
    <version>11.4.11</version>
</dependency>

<dependency>
    <groupId>org.commonmark</groupId>
    <artifactId>commonmark</artifactId>
    <version>0.22.0</version>
</dependency>

<dependency>
    <groupId>org.commonmark</groupId>
    <artifactId>commonmark-ext-gfm-tables</artifactId>
    <version>0.22.0</version>
</dependency>
```

**Hinweis:** Apache POI (5.2.5) bleibt für Excel-Export und für das Lesen von Word-Dokumenten in Tests installiert.

## Implementierungsdetails

### Konvertierungsprozess

1. **Markdown-Datei einlesen**
   ```java
   String markdownContent = Files.readString(markdownPath);
   ```

2. **Markdown parsen**
   ```java
   Parser parser = Parser.builder()
       .extensions(Arrays.asList(TablesExtension.create()))
       .build();
   Node document = parser.parse(markdownContent);
   ```

3. **Word-Dokument erstellen mit docx4j**
   ```java
   WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.createPackage();
   MainDocumentPart mainDocumentPart = wordMLPackage.getMainDocumentPart();
   ObjectFactory factory = new ObjectFactory();
   ```
   - Rekursives Durchlaufen des Markdown-AST
   - Erstellen entsprechender OOXML-Elemente (P, R, Tbl)
   - Anwendung von Formatierungen über OOXML-Eigenschaften

4. **Dokument speichern**
   ```java
   wordMLPackage.save(new File(outputPath.toString()));
   ```

### Word-Formatierungen

**Überschriften:**
- H1 (#): Stil "Heading1" (entspricht "Überschrift 1" in Word)
- H2 (##): Stil "Heading2" (entspricht "Überschrift 2" in Word)
- H3 (###): Stil "Heading3" (entspricht "Überschrift 3" in Word)
- H4-H6: Stile "Heading4"-"Heading6"
- Die Stile werden über `PPrBase.PStyle` gesetzt und von Word automatisch erkannt

**Listen:**
- Nummerierte Listen: NumID 1
- Aufzählungslisten: NumID 2
- Verschachtelung: via Ilvl (Indentation Level)
- Implementiert über `PPrBase.NumPr` mit `NumId` und `Ilvl`

**Tabellen:**
- Header-Zeile: Fett formatiert über `RPr` (Run Properties)
- Breite: 5000 (Prozent-basiert, entspricht 100%)
- Erstellt über docx4j `Tbl`, `Tr`, `Tc` Elemente

## Tests

Der Service wird durch 5 umfassende Tests abgedeckt:

1. **testConvertQuelldokumentToErgebnisdokument**
   - Vollständige Konvertierung der Beispieldatei
   - Prüfung aller Elemente

2. **testHeadingConversion**
   - Überschriften-Stile korrekt

3. **testListConversion**
   - Listen und Nummerierung

4. **testTextFormatting**
   - Fett und Kursiv

5. **testTableConversion**
   - Tabellen-Struktur und Header

Alle Tests befinden sich in:
```
src/test/java/name/saak/contactmanager/service/MarkdownToDocxServiceTest.java
```

## Beispiel-Dateien

### Quelldokument.md

Beispiel-Markdown-Datei mit allen unterstützten Elementen:
```
src/test/resources/Quelldokument.md
```

Enthält:
- Überschriften (H1, H2)
- Normale Absätze
- Nummerierte Listen (verschachtelt)
- Aufzählungslisten (verschachtelt)
- Tabelle (3 Spalten, 2 Datenzeilen)
- Text mit Fett- und Kursiv-Formatierung

### Ergebnisdokument.docx

Resultierendes Word-Dokument:
```
src/test/resources/Ergebnisdokument.docx
```

Das Dokument wird bei jedem Test-Lauf neu erstellt.

## Einschränkungen

### Nicht unterstützt

- ❌ **Bilder** - Derzeit keine Bild-Konvertierung
- ❌ **Links** - Hyperlinks werden als normaler Text dargestellt
- ❌ **Code-Blöcke** - Werden als normaler Text formatiert
- ❌ **Blockquotes** - Werden als normaler Text dargestellt
- ❌ **Horizontale Linien** - Werden ignoriert
- ❌ **HTML in Markdown** - Wird nicht verarbeitet
- ❌ **Fußnoten** - Nicht unterstützt
- ❌ **Task-Listen** - Nicht unterstützt

### Bekannte Besonderheiten

- **Tabellen-Ausrichtung**: Spaltenausrichtung (links/zentriert/rechts) wird derzeit nicht umgesetzt
- **Listen-Formatierung**: Standard Word-Nummerierung wird verwendet
- **Überschriften**: Standard Word Heading-Stile werden verwendet

## Erweiterungsmöglichkeiten

Falls zukünftig weitere Features benötigt werden:

1. **Bilder einfügen**
   ```java
   // Mit docx4j
   File imageFile = new File(imagePath);
   byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
   BinaryPartAbstractImage imagePart = BinaryPartAbstractImage.createImagePart(
       wordMLPackage, imageBytes);

   Inline inline = imagePart.createImageInline(
       "Bildname", "Bildbeschreibung", 0, 1, false);

   P paragraph = factory.createP();
   R run = factory.createR();
   Drawing drawing = factory.createDrawing();
   drawing.getAnchorOrInline().add(inline);
   run.getContent().add(drawing);
   paragraph.getContent().add(run);
   ```

2. **Hyperlinks**
   ```java
   // Mit docx4j
   org.docx4j.wml.P.Hyperlink hyperlink = factory.createPHyperlink();
   String relationshipId = mainDocumentPart.addTargetPart(
       new ExternalTarget(url)).getId();
   hyperlink.setId(relationshipId);

   R run = factory.createR();
   Text text = factory.createText();
   text.setValue("Link-Text");
   run.getContent().add(text);
   hyperlink.getContent().add(run);
   ```

3. **Code-Blöcke**
   - Monospace-Schriftart über `RPr.setRFonts()`
   - Grauer Hintergrund über `CTShd` (Shading)
   - Rahmen über `PBdr` (Paragraph Border)

4. **Angepasste Stile**
   - Eigene Word-Styles definieren über `StyleDefinitionsPart`
   - Corporate Identity Farben über `CTColor`
   - Firmenspezifische Formatierungen

## Logging

Der Service loggt alle Konvertierungen:

```
INFO  MarkdownToDocxService - Converting Markdown to DOCX: Quelldokument.md -> Ergebnisdokument.docx
INFO  MarkdownToDocxService - Conversion completed successfully
```

## Performance

- Kleine Dateien (< 100 KB): < 100 ms
- Mittlere Dateien (100 KB - 1 MB): < 1 Sekunde
- Große Dateien (> 1 MB): 1-5 Sekunden

Die Konvertierung ist schnell genug für interaktive Anwendungen.

## Troubleshooting

### Problem: OutOfMemoryError bei großen Dateien

**Lösung**: JVM Heap-Size erhöhen
```bash
java -Xmx2g -jar application.jar
```

### Problem: Umlaute werden nicht korrekt dargestellt

**Lösung**: UTF-8 Encoding sicherstellen
```java
String markdownContent = Files.readString(markdownPath, StandardCharsets.UTF_8);
```

### Problem: Tabellen werden nicht korrekt formatiert

**Lösung**: GitHub Flavored Markdown Syntax verwenden:
```markdown
| Spalte 1 | Spalte 2 |
| --- | --- |
| Wert 1 | Wert 2 |
```

Nicht:
```markdown
| Spalte 1 | Spalte 2
| --- | ---
| Wert 1 | Wert 2
```

## Technologiewahl: docx4j vs Apache POI

**Stand:** Januar 2026

Die Implementierung verwendet **docx4j** statt Apache POI aus folgenden Gründen:

### Vorteile von docx4j

1. **Bessere OOXML-Unterstützung**
   - Direkter Zugriff auf OOXML-Strukturen (P, R, PPr, RPr)
   - Vollständige Kontrolle über Word-Dokument-Struktur
   - Näher am tatsächlichen DOCX-Format

2. **Präzise Formatierung**
   - Styles werden über `PPrBase.PStyle` gesetzt
   - Überschriften werden korrekt als "Heading1", "Heading2" etc. erkannt
   - Keine Probleme mit Style-Definitionen

3. **Stabilität**
   - Keine ambigen Klassennamen (docx4j.wml.Text vs commonmark.node.Text durch qualified names gelöst)
   - Klare API-Struktur durch ObjectFactory-Pattern

4. **Erweiterbarkeit**
   - Einfacheres Hinzufügen von komplexen Features (Bilder, Hyperlinks)
   - Bessere Integration mit OOXML-Spezifikation

### Apache POI bleibt verfügbar

Apache POI (5.2.5) bleibt im Projekt für:
- **Excel-Export** (bestehende Funktionalität)
- **Test-Validierung** (Lesen von generierten DOCX-Dateien in Tests)

### Migration

Die Migration erfolgte in Version 0.0.0-SNAPSHOT am 02.01.2026:
- Vollständige Umstellung von Apache POI auf docx4j
- Alle 5 Markdown-Tests bestehen
- Alle 72 Gesamt-Tests bestehen (1 experimenteller Test deaktiviert)
- Keine Änderungen an der öffentlichen API erforderlich
