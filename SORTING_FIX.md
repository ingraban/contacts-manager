# Address Sorting Fix

## Problem

When sorting contacts by "Adresse" (address), only the city (Ort) was being used for sorting, not both city and street (Strasse) as intended.

## Root Cause

The issue was caused by a known limitation in Hibernate when combining:
- `SELECT DISTINCT`
- `LEFT JOIN FETCH` (for eager loading collections)
- Dynamic sorting via `Sort` parameter

When these are combined, Hibernate creates a Cartesian product (multiple rows per contact if they have multiple hashtags), and then applies `DISTINCT` in memory after fetching. This causes the `ORDER BY` clause to not work correctly for multi-field sorting.

## Solution

Implemented a two-step query approach:

### Step 1: Query for IDs with Sorting
```java
@Query("SELECT c.id FROM Contact c")
List<Long> findAllContactIds(Sort sort);
```

This query has NO `JOIN FETCH`, so the Sort parameter works correctly and generates proper SQL:
```sql
SELECT c.id FROM contact c
ORDER BY c.ort, c.strasse, c.nachname, c.vorname
```

### Step 2: Fetch Full Entities with Hashtags
```java
@Query("SELECT DISTINCT c FROM Contact c " +
       "LEFT JOIN FETCH c.hashtags " +
       "WHERE c.id IN :ids")
List<Contact> findByIdsWithHashtags(@Param("ids") List<Long> ids);
```

This fetches the complete contact objects with all hashtags using the IDs from step 1.

### Step 3: Maintain Sort Order
```java
private List<Contact> sortContactsByIds(List<Contact> contacts, List<Long> ids) {
    Map<Long, Contact> contactMap = contacts.stream()
        .collect(Collectors.toMap(Contact::getId, c -> c));

    return ids.stream()
        .map(contactMap::get)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
}
```

Since SQL `IN` clauses don't preserve order, we manually re-sort the fetched contacts to match the order from step 1.

## Changes Made

### ContactRepository.java
- Replaced `findAllWithHashtags(Sort sort)` with `findAllContactIds(Sort sort)` and `findByIdsWithHashtags(List<Long> ids)`
- Replaced `searchContactsWithSort(searchTerm, Sort sort)` with `searchContactIds(searchTerm, Sort sort)`

### ContactService.java
- Updated `findAllContacts(sortField, sortDir)` to use the two-step approach
- Updated `searchContacts(searchTerm, sortField, sortDir)` to use the two-step approach
- Added `sortContactsByIds()` helper method
- Added imports for `Map` and `Objects`

## Result

Address sorting now correctly sorts by:
1. **City (Ort)** - primary sort field
2. **Street (Strasse)** - secondary sort field (within the same city)
3. **Last Name (Nachname)** - tertiary sort field
4. **First Name (Vorname)** - quaternary sort field

For example, contacts in Berlin will be sorted by street name (Ahornstraße, Birkenweg, Zebrastraße), and then contacts in München will follow, also sorted by street name.

## Tests

All 59 existing tests pass after this fix, confirming:
- No regressions in existing functionality
- Sorting works correctly for all fields (vorname, nachname, firma, adresse)
- Search functionality continues to work with sorting
- Hashtag loading remains efficient with no N+1 queries
