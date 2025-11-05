# Drag and Drop Backend Implementation

## Summary
Successfully implemented the backend API for drag-and-drop category reordering functionality as specified in the API documentation.

## What Was Implemented

### 1. New DTO
**File:** `src/main/java/io/learn/lexigeek/category/dto/UpdateCategoryPositionForm.java`
- Request DTO for position update endpoint
- Fields:
  - `parentUuid` (UUID, nullable): New parent category UUID or null for root level
  - `position` (Integer, required): New position among siblings (0-based, validated >= 0)

### 2. New Exception Class
**File:** `src/main/java/io/learn/lexigeek/common/exception/ValidationException.java`
- Created ValidationException following the existing exception pattern
- Extends ErrorDtoException
- Used for circular reference validation errors

### 3. Error Code Addition
**File:** `src/main/java/io/learn/lexigeek/common/validation/ErrorCodes.java`
- Added `CIRCULAR_REFERENCE_ERROR` enum value
- Used when a category cannot be moved to its own descendant

### 4. Repository Methods
**File:** `src/main/java/io/learn/lexigeek/category/domain/CategoryRepository.java`

Added methods for bulk position updates:

```java
// Decrements positions after a category is removed from old position
void decrementPositionsAfter(UUID languageUuid, UUID parentUuid, Integer position)

// Increments positions to make room for category in new position
void incrementPositionsFrom(UUID languageUuid, UUID parentUuid, Integer position, UUID excludeUuid)

// Finds categories by parent UUID (for potential future use)
List<Category> findByParentUuid(UUID parentUuid)

// Finds category with parent eager loading (for potential future use)
Optional<Category> findByUuidAndLanguageUuidWithParent(UUID uuid, UUID languageUuid)
```

### 5. Facade Interface Update
**File:** `src/main/java/io/learn/lexigeek/category/CategoryFacade.java`
- Added method signature:
  ```java
  void updateCategoryPosition(UUID languageUuid, UUID uuid, UpdateCategoryPositionForm form)
  ```

### 6. Service Implementation
**File:** `src/main/java/io/learn/lexigeek/category/domain/CategoryService.java`

Implemented `updateCategoryPosition()` method with:

#### Key Features:
- **Authorization Check**: Verifies language ownership via `languageFacade.verifyLanguageOwnership()`
- **Category Validation**: Ensures category exists in specified language
- **Parent Validation**: Validates parent category exists if provided
- **Circular Reference Prevention**: Checks if move would create circular reference
- **Transactional Position Reordering**:
  1. Closes gap in old position (decrements siblings)
  2. Makes room in new position (increments siblings)
  3. Updates the moved category

#### Circular Reference Detection:
```java
private boolean wouldCreateCircularReference(UUID categoryUuid, UUID newParentUuid)
```
- Traverses parent chain up to 100 levels
- Returns true if categoryUuid found in parent chain
- Prevents infinite loops with max depth check

### 7. Controller Endpoint
**File:** `src/main/java/io/learn/lexigeek/category/controller/CategoryController.java`

Added PATCH endpoint:
```
PATCH /languages/{languageUuid}/categories/{uuid}/position
```

**Details:**
- Returns `204 No Content` on success
- Validates request body using `@Valid` annotation
- Route constant: `CATEGORY_POSITION`

## API Endpoint Usage

### Request
```http
PATCH /languages/{languageUuid}/categories/{categoryUuid}/position
Content-Type: application/json

{
  "parentUuid": "parent-category-uuid" | null,
  "position": 0
}
```

### Response Codes
- `204 No Content` - Success
- `400 Bad Request` - Validation error (e.g., circular reference, negative position)
- `403 Forbidden` - User doesn't own the language
- `404 Not Found` - Category or parent not found

## Examples

### Move Category to New Parent
```json
{
  "parentUuid": "789e0123-e89b-12d3-a456-426614174000",
  "position": 0
}
```

### Move Category to Root Level
```json
{
  "parentUuid": null,
  "position": 2
}
```

### Reorder Within Same Parent
```json
{
  "parentUuid": "current-parent-uuid",
  "position": 3
}
```

## Validation Rules Implemented

1. ✅ **Position must be non-negative** - Validated via `@Min(0)` annotation
2. ✅ **Position must not be null** - Validated via `@NotNull` annotation
3. ✅ **Parent must exist** - Throws `NotFoundException` with `PARENT_CATEGORY_NOT_FOUND`
4. ✅ **Category must exist** - Throws `NotFoundException` with `CATEGORY_NOT_FOUND`
5. ✅ **User must own language** - Checked via `languageFacade.verifyLanguageOwnership()`
6. ✅ **No circular references** - Throws `ValidationException` with `CIRCULAR_REFERENCE_ERROR`

## Transaction Safety

The `@Transactional` annotation ensures that all position updates are atomic:
- If any step fails, all changes are rolled back
- Database remains in consistent state
- No partial updates or orphaned positions

## Position Reordering Logic

### Scenario: Move category from position 2 to position 0 in different parent

**Before:**
```
Old Parent (UUID: A):
  - Child1 (pos: 0)
  - Child2 (pos: 1)
  - MovingChild (pos: 2) ← Being moved
  - Child3 (pos: 3)

New Parent (UUID: B):
  - Child4 (pos: 0)
  - Child5 (pos: 1)
```

**After:**
```
Old Parent (UUID: A):
  - Child1 (pos: 0)
  - Child2 (pos: 1)
  - Child3 (pos: 2) ← Shifted down

New Parent (UUID: B):
  - MovingChild (pos: 0) ← Inserted here
  - Child4 (pos: 1) ← Shifted up
  - Child5 (pos: 2) ← Shifted up
```

## Testing Recommendations

### Unit Tests
1. Test moving category to new parent
2. Test moving category to root (parentUuid = null)
3. Test reordering within same parent
4. Test circular reference detection
5. Test invalid parent UUID
6. Test unauthorized access
7. Test negative position (should fail validation)

### Integration Tests
1. Test complete drag-and-drop flow
2. Test multiple concurrent moves
3. Test transaction rollback on error
4. Verify positions are sequential after moves

## Compilation Status
✅ **BUILD SUCCESSFUL** - All files compiled without errors

## Files Modified/Created

### Created:
1. `UpdateCategoryPositionForm.java` - Request DTO
2. `ValidationException.java` - Exception class
3. `DRAG_AND_DROP_IMPLEMENTATION.md` - This documentation

### Modified:
1. `CategoryController.java` - Added PATCH endpoint
2. `CategoryService.java` - Added position update logic
3. `CategoryRepository.java` - Added bulk update queries
4. `CategoryFacade.java` - Added interface method
5. `ErrorCodes.java` - Added CIRCULAR_REFERENCE_ERROR

## Next Steps (Optional Enhancements)

1. **Add Integration Tests** - Test the full drag-and-drop flow
2. **Add Audit Logging** - Track category movements for history
3. **Optimize Queries** - Consider batch updates for better performance
4. **Add WebSocket Support** - Real-time updates for multiple users
5. **Cache Invalidation** - Clear category tree cache after position changes

## Notes

- The implementation follows the Spring Boot best practices
- All database operations are within transactions
- The code follows the existing project structure and naming conventions
- Validation is handled at both DTO level (annotations) and service level (business logic)
- The API matches exactly what was specified in the frontend documentation

