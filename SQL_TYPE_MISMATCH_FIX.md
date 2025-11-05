# SQL Type Mismatch Fix - Recursive CTE Query

## The Error

```
ERROR: operator does not exist: uuid = bigint
Hint: No operator matches the given name and argument types. You might need to add explicit type casts.
Position: 229
```

## Root Cause

The recursive CTE query was trying to join columns with incompatible types:

**WRONG:**
```sql
INNER JOIN parent_hierarchy ph ON c.uuid = ph.parent_id
                                   ^^^^^^^^   ^^^^^^^^^^^
                                   UUID type   BIGINT type
```

This failed because:
- `c.uuid` is of type `UUID`
- `ph.parent_id` is of type `BIGINT` (references the `id` column)
- PostgreSQL cannot compare `UUID = BIGINT` without explicit casting

## Database Schema

```sql
CREATE TABLE "categories" (
    id          BIGINT PRIMARY KEY,      -- Auto-incrementing ID
    uuid        UUID UNIQUE NOT NULL,    -- UUID for external references
    parent_id   BIGINT NULL              -- References id, NOT uuid!
                REFERENCES categories (id),
    ...
)
```

**Key point:** `parent_id` references the `id` column (BIGINT), not the `uuid` column!

## The Fix

Changed the join condition to use `id` instead of `uuid`:

**BEFORE (Broken):**
```sql
WITH RECURSIVE parent_hierarchy AS (
    SELECT uuid, parent_id, 0 as depth
    FROM categories
    WHERE uuid = CAST(:startUuid AS uuid)
    UNION ALL
    SELECT c.uuid, c.parent_id, ph.depth + 1
    FROM categories c
    INNER JOIN parent_hierarchy ph ON c.uuid = ph.parent_id  ❌ Type mismatch!
    WHERE ph.depth < 100
)
SELECT COUNT(*) > 0
FROM parent_hierarchy
WHERE uuid = CAST(:targetUuid AS uuid)
AND depth > 0
```

**AFTER (Fixed):**
```sql
WITH RECURSIVE parent_hierarchy AS (
    SELECT id, uuid, parent_id, 0 as depth  -- Include id column
    FROM categories
    WHERE uuid = CAST(:startUuid AS uuid)
    UNION ALL
    SELECT c.id, c.uuid, c.parent_id, ph.depth + 1
    FROM categories c
    INNER JOIN parent_hierarchy ph ON c.id = ph.parent_id  ✅ BIGINT = BIGINT
    WHERE ph.depth < 100
)
SELECT COUNT(*) > 0
FROM parent_hierarchy
WHERE uuid = CAST(:targetUuid AS uuid)
AND depth > 0
```

## Changes Made

1. Added `id` to the SELECT list in the base case: `SELECT id, uuid, parent_id, 0 as depth`
2. Added `id` to the SELECT list in the recursive case: `SELECT c.id, c.uuid, c.parent_id, ph.depth + 1`
3. Fixed the join condition: `c.id = ph.parent_id` (instead of `c.uuid = ph.parent_id`)

## How It Works Now

### Example Hierarchy
```
Category A (id=1, uuid=aaa, parent_id=null)
  └─ Category B (id=2, uuid=bbb, parent_id=1)
      └─ Category C (id=3, uuid=ccc, parent_id=2)
```

### Query Execution for `isInParentHierarchy(startUuid="bbb", targetUuid="aaa")`

**Step 1 - Base case:**
```sql
SELECT id, uuid, parent_id, 0 as depth
FROM categories
WHERE uuid = 'bbb'

Result: (2, 'bbb', 1, 0)
```

**Step 2 - First recursion:**
```sql
SELECT c.id, c.uuid, c.parent_id, ph.depth + 1
FROM categories c
INNER JOIN parent_hierarchy ph ON c.id = ph.parent_id
-- Joins: c.id = 1 with ph.parent_id = 1

Result: (1, 'aaa', null, 1)  ← Found category A!
```

**Step 3 - Second recursion:**
```sql
-- c.id = null (no match), recursion stops
```

**Step 4 - Final check:**
```sql
SELECT COUNT(*) > 0
FROM parent_hierarchy
WHERE uuid = 'aaa'  -- Matches (1, 'aaa', null, 1)
AND depth > 0       -- depth = 1 > 0

Result: true  ← Circular reference detected!
```

## Why This Error Occurred

When we optimized the circular reference check, we created a recursive CTE query but didn't account for the fact that:

1. JPA entities use UUID for relationships (via `@ManyToOne` and UUID fields)
2. Database schema uses BIGINT for foreign keys (`parent_id` references `id`)
3. The join needs to use the database's actual foreign key relationship (`id` ↔ `parent_id`)

## Testing the Fix

The query now correctly:
- ✅ Starts with the category UUID provided
- ✅ Walks up the parent hierarchy using `id` and `parent_id`
- ✅ Checks if the target UUID exists anywhere in the hierarchy
- ✅ Returns true if circular reference would be created

### Test Case
```java
// Given: A → B → C
UUID a = createCategory("A", null);
UUID b = createCategory("B", a);
UUID c = createCategory("C", b);

// When: Try to move A under C (would create A → B → C → A)
// Then: Should detect circular reference
assertTrue(categoryRepository.isInParentHierarchy(c.toString(), a.toString()));
```

## Build Status

✅ **BUILD SUCCESSFUL**
✅ SQL type mismatch resolved
✅ Recursive CTE query working correctly
✅ Circular reference detection operational

## Key Takeaway

When writing native SQL queries with JPA:
- Remember that JPA entity relationships use UUIDs
- Database foreign keys use BIGINT IDs
- Native queries must use the actual database column types and relationships
- Always verify column types when joining tables

