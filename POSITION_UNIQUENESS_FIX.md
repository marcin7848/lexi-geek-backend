# Position Uniqueness Fix for Drag and Drop

## Problem Identified

The original implementation had a critical issue where categories could end up with duplicate positions, especially when moving a category within the same parent. This happened because:

1. When closing the gap in the old position, the moving category wasn't excluded
2. The logic didn't properly handle same-parent moves with range-based updates

## Solution Implemented

### 1. Fixed Repository Queries

**Added `excludeUuid` parameter to `decrementPositionsAfter`:**
```java
// BEFORE (WRONG - could create duplicates)
void decrementPositionsAfter(UUID languageUuid, UUID parentUuid, Integer position)

// AFTER (CORRECT - excludes the moving category)
void decrementPositionsAfter(UUID languageUuid, UUID parentUuid, Integer position, UUID excludeUuid)
```

**Added new range-based methods:**
```java
// For moving UP within same parent (shift siblings UP between newPos and oldPos)
void incrementPositionsBetween(UUID languageUuid, UUID parentUuid, 
                               Integer fromPosition, Integer toPosition, UUID excludeUuid)

// For moving DOWN within same parent (shift siblings DOWN between oldPos and newPos)
void decrementPositionsBetween(UUID languageUuid, UUID parentUuid, 
                               Integer fromPosition, Integer toPosition, UUID excludeUuid)
```

### 2. Improved Service Logic

**Added early return for no-op moves:**
```java
// If moving to same position in same parent, do nothing
if (sameParent && oldPosition.equals(newPosition)) {
    return;
}
```

**Separate handling for same-parent vs different-parent moves:**

#### Same Parent Move (Reordering)
- **Moving UP** (e.g., position 3 → position 1):
  - Categories at positions [1, 2, 3) shift UP by 1 → [2, 3, 4)
  - Moving category goes to position 1
  
- **Moving DOWN** (e.g., position 1 → position 3):
  - Categories at positions (1, 2, 3] shift DOWN by 1 → (0, 1, 2]
  - Moving category goes to position 3

#### Different Parent Move
- Close gap in old parent (all categories after old position shift down by 1)
- Make room in new parent (all categories >= new position shift up by 1)
- Update the category with new parent and position

## Position Uniqueness Guarantee

### How Uniqueness is Maintained

1. **Always Exclude Moving Category**: All position update queries exclude the moving category using `c.uuid != :excludeUuid`

2. **Range-Based Updates**: Same-parent moves only affect categories in the range between old and new positions

3. **Atomic Transactions**: All position updates happen in a single `@Transactional` method, ensuring consistency

4. **No Gaps in Logic**: Every scenario (up, down, same parent, different parent) has specific handling

## Examples

### Example 1: Moving Within Same Parent (UP)

**Initial State:**
```
Parent A:
├─ Cat1 (pos: 0)
├─ Cat2 (pos: 1)
├─ Cat3 (pos: 2)  ← Moving this
├─ Cat4 (pos: 3)
└─ Cat5 (pos: 4)
```

**Move Cat3 from position 2 to position 1:**

1. Query: `incrementPositionsBetween(languageUuid, parentA, 1, 2, cat3Uuid)`
   - Categories with position >= 1 AND position < 2 AND uuid != cat3
   - Affected: Cat2 (position 1)
   - Cat2: position 1 → 2

2. Update Cat3: position 2 → 1

**Final State:**
```
Parent A:
├─ Cat1 (pos: 0)
├─ Cat3 (pos: 1)  ← Moved here
├─ Cat2 (pos: 2)  ← Shifted up
├─ Cat4 (pos: 3)
└─ Cat5 (pos: 4)
```

✅ **All positions unique: [0, 1, 2, 3, 4]**

### Example 2: Moving Within Same Parent (DOWN)

**Initial State:**
```
Parent A:
├─ Cat1 (pos: 0)  ← Moving this
├─ Cat2 (pos: 1)
├─ Cat3 (pos: 2)
├─ Cat4 (pos: 3)
└─ Cat5 (pos: 4)
```

**Move Cat1 from position 0 to position 3:**

1. Query: `decrementPositionsBetween(languageUuid, parentA, 0, 3, cat1Uuid)`
   - Categories with position > 0 AND position <= 3 AND uuid != cat1
   - Affected: Cat2 (pos: 1), Cat3 (pos: 2), Cat4 (pos: 3)
   - Cat2: position 1 → 0
   - Cat3: position 2 → 1
   - Cat4: position 3 → 2

2. Update Cat1: position 0 → 3

**Final State:**
```
Parent A:
├─ Cat2 (pos: 0)  ← Shifted down
├─ Cat3 (pos: 1)  ← Shifted down
├─ Cat4 (pos: 2)  ← Shifted down
├─ Cat1 (pos: 3)  ← Moved here
└─ Cat5 (pos: 4)
```

✅ **All positions unique: [0, 1, 2, 3, 4]**

### Example 3: Moving to Different Parent

**Initial State:**
```
Parent A:                Parent B:
├─ Cat1 (pos: 0)        ├─ Cat4 (pos: 0)
├─ Cat2 (pos: 1)        └─ Cat5 (pos: 1)
├─ Cat3 (pos: 2)  ← Moving
└─ Cat4 (pos: 3)
```

**Move Cat3 from Parent A (position 2) to Parent B (position 1):**

1. Query: `decrementPositionsAfter(languageUuid, parentA, 2, cat3Uuid)`
   - Categories in Parent A with position > 2 AND uuid != cat3
   - Affected: Cat4 (pos: 3)
   - Cat4: position 3 → 2

2. Query: `incrementPositionsFrom(languageUuid, parentB, 1, cat3Uuid)`
   - Categories in Parent B with position >= 1 AND uuid != cat3
   - Affected: Cat5 (pos: 1)
   - Cat5: position 1 → 2

3. Update Cat3: parent = Parent B, position = 1

**Final State:**
```
Parent A:                Parent B:
├─ Cat1 (pos: 0)        ├─ Cat4 (pos: 0)
├─ Cat2 (pos: 1)        ├─ Cat3 (pos: 1)  ← Moved here
└─ Cat4 (pos: 2)        └─ Cat5 (pos: 2)  ← Shifted up
```

✅ **All positions unique in Parent A: [0, 1, 2]**
✅ **All positions unique in Parent B: [0, 1, 2]**

## Database Query Details

### incrementPositionsBetween
```sql
UPDATE categories 
SET position = position + 1 
WHERE language_uuid = ?
  AND (parent_uuid IS NULL AND ? IS NULL OR parent_uuid = ?)
  AND position >= ?        -- fromPosition (inclusive)
  AND position < ?         -- toPosition (exclusive)
  AND uuid != ?            -- excludeUuid (moving category)
```

**Use case:** Moving UP within same parent - shifts categories in range UP

### decrementPositionsBetween
```sql
UPDATE categories 
SET position = position - 1 
WHERE language_uuid = ?
  AND (parent_uuid IS NULL AND ? IS NULL OR parent_uuid = ?)
  AND position > ?         -- fromPosition (exclusive)
  AND position <= ?        -- toPosition (inclusive)
  AND uuid != ?            -- excludeUuid (moving category)
```

**Use case:** Moving DOWN within same parent - shifts categories in range DOWN

### decrementPositionsAfter
```sql
UPDATE categories 
SET position = position - 1 
WHERE language_uuid = ?
  AND (parent_uuid IS NULL AND ? IS NULL OR parent_uuid = ?)
  AND position > ?         -- position (exclusive)
  AND uuid != ?            -- excludeUuid (moving category)
```

**Use case:** Closing gap when removing from old parent

### incrementPositionsFrom
```sql
UPDATE categories 
SET position = position + 1 
WHERE language_uuid = ?
  AND (parent_uuid IS NULL AND ? IS NULL OR parent_uuid = ?)
  AND position >= ?        -- position (inclusive)
  AND uuid != ?            -- excludeUuid (moving category)
```

**Use case:** Making room when inserting into new parent

## Testing Verification

### Test Cases to Verify Uniqueness

1. ✅ **Move category up within same parent**
   - Verify no duplicate positions
   - Verify sequential positions (no gaps)

2. ✅ **Move category down within same parent**
   - Verify no duplicate positions
   - Verify sequential positions (no gaps)

3. ✅ **Move category to different parent**
   - Verify no duplicate positions in old parent
   - Verify no duplicate positions in new parent
   - Verify sequential positions in both parents

4. ✅ **Move category to root level**
   - Verify no duplicate positions at root
   - Verify old parent positions are sequential

5. ✅ **Move root category to nested level**
   - Verify no duplicate positions at root
   - Verify no duplicate positions in new parent

6. ✅ **Move to same position (no-op)**
   - Verify early return
   - Verify no database queries executed

## SQL Query to Verify Position Uniqueness

You can run this query to verify there are no duplicate positions:

```sql
SELECT 
    language_uuid,
    COALESCE(parent_uuid::text, 'ROOT') as parent_group,
    position,
    COUNT(*) as count
FROM categories
GROUP BY language_uuid, parent_uuid, position
HAVING COUNT(*) > 1;
```

**Expected result:** Empty result set (no duplicates)

## Summary of Changes

### Files Modified:

1. **CategoryRepository.java**
   - Added `excludeUuid` parameter to `decrementPositionsAfter()`
   - Added `incrementPositionsBetween()` method
   - Added `decrementPositionsBetween()` method

2. **CategoryService.java**
   - Added early return for no-op moves
   - Added same-parent detection logic
   - Separated handling for same-parent vs different-parent moves
   - Use range-based methods for same-parent moves

### Key Improvements:

✅ **Position uniqueness guaranteed** - Categories always exclude the moving category
✅ **No gaps in positions** - Range-based updates ensure sequential positions
✅ **Transaction safety** - All updates in single transaction
✅ **Optimized same-parent moves** - Only affects categories in the affected range
✅ **Clear logic flow** - Separate code paths for different scenarios

## Performance Considerations

- **Same-parent moves**: Only updates categories in the range [min(oldPos, newPos), max(oldPos, newPos)]
- **Different-parent moves**: Updates categories in both parents, but with efficient indexed queries
- **All queries use indexes**: `language_uuid`, `parent_uuid`, and `position` should be indexed
- **Single transaction**: All updates are atomic and consistent

The implementation now correctly maintains position uniqueness across all drag-and-drop scenarios! 🎉

