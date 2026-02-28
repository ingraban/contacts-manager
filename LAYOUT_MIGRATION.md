# Layout-Migration Guide

## Neues Universal-Layout mit Sidebar

Das neue `app-layout.html` bietet eine einheitliche Sidebar-Navigation für alle Bereiche.

### Sidebar-Menüpunkte

- **Kontakte** (`/contacts`)
- **Hashtags** (`/hashtags`)
- **Mitarbeiter** (`/employees`) - wenn Controller existiert
- **Abteilungen** (`/departments`) - wenn Controller existiert
- **Administration** (`/admin`) - nur für ADMIN-Rolle

## Migration einer Seite

### Vorher (Beispiel: contacts/list.html)

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Kontakte</title>
    <link rel="stylesheet" th:href="@{/css/styles.css}">
</head>
<body>
    <th:block th:replace="~{fragments/header :: header('contacts')}"></th:block>
    
    <main class="container">
        <h1>Kontakte</h1>
        <!-- Inhalt -->
    </main>
</body>
</html>
```

### Nachher (mit app-layout)

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Kontakte - Sommerhausen Office Suite</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <link rel="stylesheet" th:href="@{/css/styles.css}">
</head>
<body>
    <th:block th:replace="~{fragments/app-layout :: app-layout('contacts', ~{::content})}">
        <th:block th:fragment="content">
            <h1>Kontakte</h1>
            <!-- Inhalt hier -->
        </th:block>
    </th:block>
</body>
</html>
```

## Wichtige Änderungen

1. **Layout-Fragment verwenden**: `app-layout('contacts', ~{::content})`
   - Erster Parameter: aktiver Menüpunkt (`contacts`, `hashtags`, `employees`, `departments`)
   - Zweiter Parameter: Content-Fragment

2. **Content in Fragment wrappen**: Gesamter Seiteninhalt kommt in `<th:block th:fragment="content">`

3. **Stylesheets im Head**: Bootstrap Icons und styles.css müssen im Head eingebunden sein

4. **Container-Klasse entfernen**: Das Layout hat bereits `.admin-content-inner`, keine zusätzliche `.container` nötig

## Bestehende Templates, die migriert werden sollten

- `/templates/contacts/list.html` → `activeMenu='contacts'`
- `/templates/contacts/form.html` → `activeMenu='contacts'`
- `/templates/hashtags/list.html` → `activeMenu='hashtags'`
- `/templates/hashtags/form.html` → `activeMenu='hashtags'`
- (Zukünftig) employees/list.html → `activeMenu='employees'`
- (Zukünftig) departments/list.html → `activeMenu='departments'`

## Admin-Bereich

Bleibt unverändert und verwendet weiterhin `admin-layout`:

```html
<th:block th:replace="~{fragments/admin-layout :: admin-layout('users', ~{::content})}">
```

Admin-Layout hat eigene Menüpunkte:
- Benutzerverwaltung
- Datenbank
- Zurück zu Kontakte (Link)
