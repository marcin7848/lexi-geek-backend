# Quick Reference: Drag and Drop API Request

## Endpoint
```
PATCH /languages/{languageUuid}/categories/{categoryUuid}/position
```

## Request Body
```typescript
{
  "parentUuid": string | null,  // New parent category UUID, or null for root level
  "position": number             // New position among siblings (0-based index)
}
```

## Example Requests

### 1. Move category to a new parent
```bash
PATCH /languages/550e8400-e29b-41d4-a716-446655440000/categories/123e4567-e89b-12d3-a456-426614174000/position
Content-Type: application/json

{
  "parentUuid": "789e0123-e89b-12d3-a456-426614174000",
  "position": 0
}
```

### 2. Move category to root level
```bash
PATCH /languages/550e8400-e29b-41d4-a716-446655440000/categories/123e4567-e89b-12d3-a456-426614174000/position
Content-Type: application/json

{
  "parentUuid": null,
  "position": 2
}
```

### 3. Reorder within same parent
```bash
PATCH /languages/550e8400-e29b-41d4-a716-446655440000/categories/123e4567-e89b-12d3-a456-426614174000/position
Content-Type: application/json

{
  "parentUuid": "789e0123-e89b-12d3-a456-426614174000",
  "position": 3
}
```

## Response Codes
- `200 OK` - Success (with optional response body)
- `204 No Content` - Success (no response body)
- `400 Bad Request` - Invalid request (e.g., circular reference, invalid position)
- `403 Forbidden` - User not authorized
- `404 Not Found` - Category or parent not found

## Validation Rules (Backend Must Implement)

1. **Circular Reference Check**: Category cannot be moved to its own descendant
2. **Valid Parent**: Parent UUID must exist and belong to same language
3. **Authorization**: User must own the language
4. **Position**: Must be non-negative number

## How Frontend Uses It

The frontend calls this endpoint when a user:
- Drags a category to a different position in the tree
- Drops a category on another category (becomes sibling)
- Drops a category on a dropzone (becomes child or root)

The frontend code is already implemented in:
- Service: `src/services/categoryService.ts` → `updateCategoryPosition()`
- Component: `src/components/CategoryTree.tsx` → `handleDragEnd()`

