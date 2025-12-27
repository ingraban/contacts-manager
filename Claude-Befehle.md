# Claude Befehle

## JPA Einbauen

Erweitere das Projekt, damit wir über JPA auf eine Datenbank zugreifen können. Die Datenbank soll für den Test eine H2-Memory Datenbank mit Testdaten über ein INIT-Script sein. In der Produktion soll eine PostgreSQL verwendet werden.

Die Datenbank soll über Luiquibase aufgesetzt werden. 

Als erste Verwaltungsmaske soll ein Kontakt mit den folgenden Feldern in der Maske verwaltet werden können.

- Vorname (String)
- Nachname (String)
- Straße (String)
- Postleitzahl (String)
- Ort (String)
- Anrede (String)
- Telefon1 (String)
- Telefon2 (String)
- EMail (String)

Es soll einen Index geben, bei dem eine Volltextsuche angeboten wird. Außerdem sollen Datensätze erstellt, gelesen, geändert und gelöscht werden können.

Schreibe entsprechende Tests und führe diese durch.

1. Überarbeitung

Kannst Du die Button "Suchen" und "Zurücksetzen" in die gleiche Zeile setzen, wie den Suchstring. Außerdem solltest Du symbole (Lupe, Pfeil im Kreis verwenden). Für die "Bearbeiten" den Stift verwenden, für Löschen den Papierkorb und für neu das Plus-Zeichen

2. Überarbeitung

Verwende bitte Bootstrap Icons und stellen die textuelle beschreibung als optionale Description zur verfügung, die beim Hoverns angezeigt wird 

## Hashtag Management System Implementation Plan

### Overview

Implement a comprehensive hashtag management system with many-to-many relationship to contacts. Hashtags can be locked (not deleted) and support full-text search integration with AND logic for multiple tags.

### User Requirements

Hashtag Entity:
 - Name (String) - Instagram pattern: ^#[a-zA-Z0-9_]+$
 - Beschreibung (String) - description
 - Gesperrt (boolean) - locked flag

Business Rules:
 - Hashtags CANNOT be deleted, only locked
 - Locked hashtags are hidden in selection fields
 - Locked hashtags are hidden in entity lists
 - Case-insensitive unique names
 - Names normalized to lowercase

 Relationship:
 - Many-to-many with Contact (0-n hashtags per contact)
 - Display hashtags in contact list (non-clickable)

 Search:
 - Hashtag search requires # prefix
 - Multiple hashtags use AND logic (all must be present)
 - Example: #Familie #Freunde finds contacts with BOTH tags

 Implementation Order

 1. Database Schema (Liquibase Migrations)

 Create: 004-create-hashtags-table.yaml
 - Table: hashtag with id, name, beschreibung, gesperrt, created_at, updated_at
 - Unique constraint on name (case-insensitive)
 - Index on LOWER(name) for performance
 - Index on gesperrt for filtering
 - Default gesperrt = false

 Create: 005-create-contact-hashtag-table.yaml
 - Join table: contact_hashtag with contact_id, hashtag_id
 - Composite primary key
 - Foreign keys: CASCADE on contact, RESTRICT on hashtag
 - Indexes on both columns for performance

 Create: 006-add-test-hashtags.yaml (context=test)
 - Insert 4 test hashtags: #Familie, #Freunde, #Geschaeftlich, #Archiv (locked)

 Update: db.changelog-master.yaml - include new migrations

 2. Domain Layer

 Create: domain/Hashtag.java
 - Entity with @Table, @Id, @GeneratedValue
 - Fields: name, beschreibung, gesperrt, createdAt, updatedAt
 - Validation: @NotBlank, @Pattern (Instagram regex), @Size
 - @ManyToMany(mappedBy="hashtags") with Contact
 - @PrePersist: normalize name to lowercase, set timestamps
 - @PreUpdate: normalize name, update timestamp
 - equals/hashCode based on lowercase name (business key)

 Update: domain/Contact.java
 - Add @ManyToMany with @JoinTable
 - JoinTable: name="contact_hashtag", joinColumns, inverseJoinColumns
 - FetchType.LAZY, cascade PERSIST/MERGE only
 - Helper methods: addHashtag(), removeHashtag() (bidirectional sync)

 3. Repository Layer

 Create: repository/HashtagRepository.java
 - Extends JpaRepository<Hashtag, Long>
 - findByNameIgnoreCase(String) - case-insensitive name lookup
 - findByGesperrtFalseOrderByNameAsc() - active hashtags only
 - findAllByOrderByNameAsc() - all hashtags for admin
 - searchHashtags(String) - LIKE search on name/description
 - existsByNameIgnoreCaseAndIdNot(String, Long) - duplicate check excluding self

 Update: repository/ContactRepository.java
 - findByAllHashtags(List<String>, long) - AND logic for multiple hashtags
   - JOIN hashtags, filter by names and not locked
   - GROUP BY contact, HAVING COUNT = number of hashtags
 - findByIdWithActiveHashtags(Long) - fetch with non-locked hashtags (avoid N+1)

 4. Service Layer

 Create: service/HashtagService.java
 - @Service @Transactional
 - Methods:
   - findAllHashtags() - all, sorted
   - findActiveHashtags() - non-locked only
   - findHashtagById(Long) - single by ID
   - searchHashtags(String) - search with fallback to all
   - createHashtag(Hashtag) - validate uniqueness, save
   - updateHashtag(Long, Hashtag) - validate uniqueness (excluding self), update
   - lockHashtag(Long) - set gesperrt=true
   - unlockHashtag(Long) - set gesperrt=false
   - validateUniqueHashtagName(String, Long) - throws DuplicateHashtagException
 - Exceptions: HashtagNotFoundException, DuplicateHashtagException

 Update: service/ContactService.java
 - Inject HashtagRepository
 - Update searchContacts(String):
   - Check if searchTerm starts with #
   - If yes, call searchByHashtags(searchTerm)
   - If no, call existing full-text search
 - Add searchByHashtags(String):
   - Parse hashtag names from search term (split by space, filter #)
   - Convert to lowercase
   - Call repository.findByAllHashtags with AND logic
 - Update createContact(Contact, Set<Long>):
   - Accept optional hashtagIds parameter
   - Load hashtags by IDs (filter locked ones)
   - Set hashtags on contact before save
 - Update updateContact(Long, Contact, Set<Long>):
   - Clear existing hashtags
   - Load and set new hashtags (filter locked ones)

 5. Controller Layer

 Create: controller/HashtagController.java
 - @Controller @RequestMapping("/hashtags")
 - Endpoints:
   - GET /hashtags - list all with optional search
   - GET /hashtags/new - show create form
   - POST /hashtags - create with validation
   - GET /hashtags/{id}/edit - show edit form
   - POST /hashtags/{id} - update with validation
   - POST /hashtags/{id}/lock - lock hashtag
   - POST /hashtags/{id}/unlock - unlock hashtag
 - Exception handler for HashtagNotFoundException
 - Model attributes: hashtag, hashtags, searchTerm, isEdit, messages

 Update: controller/ContactController.java
 - Inject HashtagService
 - Update showCreateForm():
   - Add availableHashtags to model (findActiveHashtags)
 - Update createContact():
   - Accept @RequestParam Set hashtagIds
   - Pass to service.createContact
   - On error, re-add availableHashtags to model
 - Update showEditForm():
   - Add availableHashtags to model
 - Update updateContact():
   - Accept @RequestParam Set hashtagIds
   - Pass to service.updateContact
   - On error, re-add availableHashtags to model

 6. View Layer

 Create: templates/hashtags/list.html
 - Reuse search form pattern from contacts (Variante 2 style)
 - Table columns: Name, Beschreibung, Status (badge), Kontakte count, Aktionen
 - Status badges: "Aktiv" (green) or "Gesperrt" (yellow)
 - Actions: Edit (pencil), Lock (lock icon), Unlock (unlock icon)
 - Lock confirmation dialog
 - Create button with plus icon

 Create: templates/hashtags/form.html
 - Reusable form for create/edit
 - Fields: Name (required, pattern validation), Beschreibung (textarea)
 - Validation error display
 - Note about pattern requirements
 - Submit/Cancel buttons with icons

 Update: templates/contacts/list.html
 - Add hashtag display in name column:
   - New div with class="hashtag-list"
   - Loop through contact.hashtags (filter !gesperrt)
   - Display as badges with class="hashtag-badge"
 - Add CSS for badges:
   - .hashtag-list - flex wrap with gap
   - .hashtag-badge - green background, rounded, small font
   - .badge, .badge-success, .badge-warning - status badges

 Update: templates/contacts/form.html
 - Add hashtag selection section after email field
 - Display available hashtags as checkboxes
 - Grid layout for checkboxes (auto-fill, 150px min)
 - Pre-select hashtags that contact already has
 - Note about hashtag selection
 - CSS for .hashtag-selection, .checkbox-item

 Update: templates/fragments/header.html
 - Add Hashtags navigation link between Kontakte and Abmelden

 Update: static/css/styles.css
 - Add hashtag-specific styles (badges, selection, checkboxes)

 7. Testing

 Create: test/repository/HashtagRepositoryTest.java
 - @DataJpaTest, @ActiveProfiles("test")
 - Tests: save/find, findByName (case-insensitive), find active only, search, unique constraint

 Create: test/service/HashtagServiceTest.java
 - @ExtendWith(MockitoExtension), Mock HashtagRepository
 - Tests: CRUD operations, lock/unlock, duplicate detection, search

 Create: test/controller/HashtagControllerTest.java
 - @SpringBootTest, @AutoConfigureMockMvc, @ActiveProfiles("test"), @Transactional
 - Tests: list, create form/submit, edit form/submit, lock/unlock, validation

 Update: test/repository/ContactRepositoryTest.java
 - Add tests for findByAllHashtags (single tag, multiple tags AND logic)
 - Test locked hashtags exclusion

 Update: test/service/ContactServiceTest.java
 - Test createContact with hashtags
 - Test updateContact with hashtags
 - Test searchContacts with # prefix (hashtag search)
 - Test locked hashtags are excluded

 Update: test/controller/ContactControllerTest.java
 - Test hashtag display in list
 - Test hashtag selection in create/edit forms
 - Test hashtag search functionality

 Critical Files

 New Files:
 - db/changelog/changes/004-create-hashtags-table.yaml
 - db/changelog/changes/005-create-contact-hashtag-table.yaml
 - db/changelog/changes/006-add-test-hashtags.yaml
 - domain/Hashtag.java
 - repository/HashtagRepository.java
 - service/HashtagService.java
 - controller/HashtagController.java
 - templates/hashtags/list.html
 - templates/hashtags/form.html
 - test/repository/HashtagRepositoryTest.java
 - test/service/HashtagServiceTest.java
 - test/controller/HashtagControllerTest.java

 Modified Files:
 - db/changelog/db.changelog-master.yaml
 - domain/Contact.java
 - repository/ContactRepository.java
 - service/ContactService.java
 - controller/ContactController.java
 - templates/contacts/list.html
 - templates/contacts/form.html
 - templates/fragments/header.html
 - static/css/styles.css
 - test/repository/ContactRepositoryTest.java
 - test/service/ContactServiceTest.java
 - test/controller/ContactControllerTest.java

 Key Design Decisions

 1. Soft Delete Pattern: Hashtags use gesperrt flag instead of physical deletion
 2. Case-Insensitive Uniqueness: Names stored and compared in lowercase
 3. Name Normalization: @PrePersist/@PreUpdate enforces lowercase
 4. Lazy Loading: Many-to-many with LAZY fetch to avoid N+1 queries
 5. Cascade Strategy: PERSIST/MERGE only, no cascade delete
 6. Foreign Key Constraints: CASCADE for contacts, RESTRICT for hashtags
 7. Search Logic: AND operator for multiple hashtags (all must match)
 8. Non-Interactive Display: Hashtag badges in contact list are not clickable
 9. Pattern Validation: Regex ^#[a-zA-Z0-9_]+$ at entity level

 Validation Rules

 - Name: Required, 2-50 chars, pattern ^#[a-zA-Z0-9_]+$, unique (case-insensitive)
 - Beschreibung: Optional, max 500 chars
 - Gesperrt: Default false, cannot delete once created
 - Normalization: Names automatically converted to lowercase on save

 Testing Approach

 - Unit tests for service layer (Mockito)
 - Integration tests for repository (H2 with Liquibase test data)
 - Web layer tests with MockMvc (@SpringBootTest)
 - Test AND logic for multi-hashtag search
 - Test locked hashtag filtering
 - Verify bidirectional relationship integrity

 ## Export als Excel

Ich möchte jetzt die Kontakte als Excel Datei exportieren können.
Dazu sollen alle Kontakte, die selektiert wurden als Excel-Datei heruntergeladen werden.
Im Excel gibt es die Spalten

- Firma
- Anrede
- Vorname_Name (Bestehend aus dem Vornamen und einem Leerzeichen, wenn der Vorname existiert) und dem Nachnamen
- Straße
- PLZ/Ort (Postleitzahl und Leerzeichen, wenn Postleitzahl existiert und Ort)

Die Excel soll als XLSX Datei erstellt werden mit Überschriften.

Als Test sollen die Testdaten vollständig exportiert werden. Das Ergebnis muss inhaltlich mit der Datei src/test/resources/Export.xlsx übereinstimmen.

Führe alle Tests aus und stelle sicher, dass sie durchlaufen.
