# Drag and Drop API Implementation Guide

## Overview
This document describes the backend API endpoint required to support drag-and-drop functionality for category tree reordering in the LexiGeek application.

## Endpoint Specification

### Update Category Position
**Purpose:** Update a category's position within the tree and/or change its parent category.

**HTTP Method:** `PATCH`

**URL Pattern:** `/languages/{languageUuid}/categories/{categoryUuid}/position`

**Path Parameters:**
- `languageUuid` (string, UUID): The UUID of the language
- `categoryUuid` (string, UUID): The UUID of the category being moved

**Request Headers:**
```
Content-Type: application/json
Authorization: Bearer {token}
```

**Request Body:**
```json
{
  "parentUuid": "string | null",
  "position": "number"
}
```

**Request Body Fields:**
- `parentUuid` (string | null, required): 
  - The UUID of the new parent category
  - Set to `null` if the category should become a root-level category
  - Must be a valid category UUID that exists in the same language
- `position` (number, required): 
  - The new position/order of the category among its siblings
  - Zero-based or one-based indexing (recommend zero-based)
  - Used to determine the sort order within the same parent

**Response:**
- **Success (200 OK):**
  ```json
  {
    "message": "Category position updated successfully"
  }
  ```
  Or simply an empty 204 No Content response

- **Error (400 Bad Request):**
  ```json
  {
    "error": "Invalid request",
    "details": "Cannot move category into its own descendant"
  }
  ```

- **Error (404 Not Found):**
  ```json
  {
    "error": "Category not found"
  }
  ```

- **Error (403 Forbidden):**
  ```json
  {
    "error": "Unauthorized to modify this category"
  }
  ```

## Backend Implementation Requirements

### 1. Validation
The backend MUST validate the following:

- **Circular Reference Prevention:** Ensure that a category cannot be moved to become a descendant of itself
  - Example: Category A cannot have Category A or any of A's children as its parent
  - Recursively check the parent chain to prevent circular references

- **Valid Parent:** If `parentUuid` is not null, verify that:
  - The parent category exists
  - The parent category belongs to the same language
  - The user has permission to modify both categories

- **Position Bounds:** Validate that the position value is reasonable (e.g., >= 0)

### 2. Position Reordering
When a category is moved to a new position, the backend should:

**Option A - Simple Approach:**
- Just save the exact position provided by the frontend
- Frontend will calculate appropriate positions based on sibling count

**Option B - Auto-reorder (Recommended):**
- When a category is moved to a new parent or position:
  1. Remove the category from its old position
  2. Reorder remaining siblings in the old parent (close the gap)
  3. Insert the category at the new position
  4. Shift siblings in the new parent to make room
  5. Ensure positions are sequential and have no gaps

Example:
```
Before: Parent A has children [C1(pos:0), C2(pos:1), C3(pos:2)]
Move C1 to position 1:
After: Parent A has children [C2(pos:0), C1(pos:1), C3(pos:2)]
```

### 3. Database Transaction
The update operation should be performed within a database transaction to ensure data consistency:
```sql
BEGIN TRANSACTION;

-- Update the category being moved
UPDATE categories 
SET parent_uuid = ?, position = ?, updated_at = NOW()
WHERE uuid = ? AND language_uuid = ?;

-- Optional: Reorder other affected categories
UPDATE categories 
SET position = position + 1, updated_at = NOW()
WHERE parent_uuid = ? AND position >= ? AND uuid != ?;

COMMIT;
```

### 4. Audit/Logging
Consider logging category movements for audit purposes:
- User who made the change
- Timestamp
- Old parent and position
- New parent and position

## Frontend Implementation

The frontend implementation is already complete in:
- `src/services/categoryService.ts` - API service method `updateCategoryPosition()`
- `src/components/CategoryTree.tsx` - Drag and drop logic in `handleDragEnd()`

### Frontend Request Example
```typescript
await categoryService.updateCategoryPosition(
  "language-uuid-123", 
  "category-uuid-456",
  {
    parentUuid: "parent-category-uuid-789", // or null for root level
    position: 2
  }
);
```

## Use Cases

### Use Case 1: Move Category to Different Parent
```
Scenario: User drags "Verbs" category under "Grammar" category

Request:
PATCH /languages/abc-123/categories/verbs-uuid/position
{
  "parentUuid": "grammar-uuid",
  "position": 0
}
```

### Use Case 2: Reorder Siblings
```
Scenario: User drags "Advanced" above "Intermediate" in the same parent

Request:
PATCH /languages/abc-123/categories/advanced-uuid/position
{
  "parentUuid": "basics-uuid",
  "position": 1
}
```

### Use Case 3: Move to Root Level
```
Scenario: User drags a nested category to become a root category

Request:
PATCH /languages/abc-123/categories/nested-uuid/position
{
  "parentUuid": null,
  "position": 3
}
```

## Error Scenarios to Handle

1. **Circular Reference:**
   ```
   Request: Move category A to be child of category B, where B is already a child of A
   Response: 400 Bad Request - "Cannot create circular reference"
   ```

2. **Non-existent Parent:**
   ```
   Request: parentUuid points to a category that doesn't exist
   Response: 404 Not Found - "Parent category not found"
   ```

3. **Different Language:**
   ```
   Request: parentUuid belongs to a different language
   Response: 400 Bad Request - "Parent category must be in the same language"
   ```

4. **Unauthorized:**
   ```
   Request: User doesn't own the language
   Response: 403 Forbidden - "Unauthorized to modify this category"
   ```

## Testing Recommendations

### Backend Unit Tests
1. Test moving category to new parent
2. Test reordering within same parent
3. Test moving to root level (parentUuid = null)
4. Test circular reference prevention
5. Test invalid parent UUID
6. Test cross-language parent rejection
7. Test unauthorized access
8. Test position reordering logic
9. Test transaction rollback on error

### Integration Tests
1. Test complete drag and drop flow
2. Test multiple concurrent moves
3. Test moving category with children
4. Verify frontend receives correct updated tree structure

## Database Schema Considerations

Ensure your categories table has the following structure:
```sql
CREATE TABLE categories (
    uuid VARCHAR(36) PRIMARY KEY,
    language_uuid VARCHAR(36) NOT NULL,
    parent_uuid VARCHAR(36) NULL,
    name VARCHAR(255) NOT NULL,
    mode ENUM('DICTIONARY', 'EXERCISE') NOT NULL,
    method ENUM('QUESTION_TO_ANSWER', 'ANSWER_TO_QUESTION', 'BOTH') NOT NULL,
    position INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (language_uuid) REFERENCES languages(uuid) ON DELETE CASCADE,
    FOREIGN KEY (parent_uuid) REFERENCES categories(uuid) ON DELETE CASCADE,
    
    INDEX idx_language_parent (language_uuid, parent_uuid),
    INDEX idx_position (position)
);
```

## Performance Considerations

1. **Indexing:** Ensure proper indexes on `language_uuid`, `parent_uuid`, and `position` columns
2. **Batch Updates:** If reordering multiple siblings, use batch updates
3. **Caching:** Consider invalidating category tree cache after position changes
4. **Optimistic Locking:** Consider using version numbers to prevent concurrent update conflicts

## Security Considerations

1. **Authentication:** Verify user is authenticated
2. **Authorization:** Verify user owns the language containing the category
3. **Input Validation:** Sanitize and validate all input parameters
4. **Rate Limiting:** Consider rate limiting to prevent abuse
5. **SQL Injection Prevention:** Use parameterized queries

## Additional Notes

- The frontend already implements circular reference checking before sending the request, but backend should also validate this for security
- Frontend calculates positions based on sibling count, so backend should handle gaps in position numbers gracefully
- Consider returning the updated category tree in the response to avoid an additional GET request from the frontend

