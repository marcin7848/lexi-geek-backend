# Backend Implementation Example (Pseudo-code)

## Controller/Route Handler

```typescript
// Example in Express.js/Node.js
router.patch('/languages/:languageUuid/categories/:categoryUuid/position', 
  authenticateUser,
  async (req, res) => {
    try {
      const { languageUuid, categoryUuid } = req.params;
      const { parentUuid, position } = req.body;
      const userId = req.user.id;

      // Validate request
      if (typeof position !== 'number' || position < 0) {
        return res.status(400).json({ error: 'Invalid position value' });
      }

      // Check authorization
      const language = await Language.findById(languageUuid);
      if (!language || language.userId !== userId) {
        return res.status(403).json({ error: 'Unauthorized' });
      }

      // Check if category exists and belongs to this language
      const category = await Category.findOne({ 
        uuid: categoryUuid, 
        languageUuid 
      });
      
      if (!category) {
        return res.status(404).json({ error: 'Category not found' });
      }

      // Validate parent exists if provided
      if (parentUuid !== null) {
        const parentCategory = await Category.findOne({ 
          uuid: parentUuid, 
          languageUuid 
        });
        
        if (!parentCategory) {
          return res.status(404).json({ error: 'Parent category not found' });
        }

        // Check for circular reference
        if (await isCircularReference(categoryUuid, parentUuid)) {
          return res.status(400).json({ 
            error: 'Cannot create circular reference' 
          });
        }
      }

      // Update category position
      await updateCategoryPosition(
        languageUuid,
        categoryUuid, 
        parentUuid, 
        position
      );

      res.status(200).json({ 
        message: 'Category position updated successfully' 
      });

    } catch (error) {
      console.error('Error updating category position:', error);
      res.status(500).json({ error: 'Internal server error' });
    }
  }
);
```

## Service Layer Functions

### Check Circular Reference
```typescript
async function isCircularReference(
  categoryUuid: string, 
  newParentUuid: string
): Promise<boolean> {
  let currentUuid = newParentUuid;
  
  while (currentUuid !== null) {
    if (currentUuid === categoryUuid) {
      return true; // Circular reference detected
    }
    
    const parent = await Category.findByUuid(currentUuid);
    if (!parent) break;
    
    currentUuid = parent.parentUuid;
  }
  
  return false;
}
```

### Update Category Position
```typescript
async function updateCategoryPosition(
  languageUuid: string,
  categoryUuid: string,
  newParentUuid: string | null,
  newPosition: number
): Promise<void> {
  
  // Start database transaction
  const transaction = await db.startTransaction();
  
  try {
    // Get the category being moved
    const category = await Category.findOne(
      { uuid: categoryUuid, languageUuid },
      { transaction }
    );

    const oldParentUuid = category.parentUuid;
    const oldPosition = category.position;

    // Step 1: Remove from old position (close the gap)
    if (oldParentUuid !== null || oldPosition > 0) {
      await Category.updateMany(
        {
          languageUuid,
          parentUuid: oldParentUuid,
          position: { $gt: oldPosition }
        },
        {
          $inc: { position: -1 } // Shift down by 1
        },
        { transaction }
      );
    }

    // Step 2: Make room in new position (shift siblings up)
    await Category.updateMany(
      {
        languageUuid,
        parentUuid: newParentUuid,
        position: { $gte: newPosition }
      },
      {
        $inc: { position: 1 } // Shift up by 1
      },
      { transaction }
    );

    // Step 3: Update the moved category
    await Category.updateOne(
      { uuid: categoryUuid, languageUuid },
      {
        $set: {
          parentUuid: newParentUuid,
          position: newPosition,
          updatedAt: new Date()
        }
      },
      { transaction }
    );

    // Commit transaction
    await transaction.commit();

  } catch (error) {
    // Rollback on error
    await transaction.rollback();
    throw error;
  }
}
```

## SQL Example (for SQL databases)

```sql
-- Check circular reference (recursive CTE)
WITH RECURSIVE parent_chain AS (
  -- Base case: start with the new parent
  SELECT uuid, parent_uuid, 0 as depth
  FROM categories
  WHERE uuid = :newParentUuid
  
  UNION ALL
  
  -- Recursive case: get parent's parent
  SELECT c.uuid, c.parent_uuid, pc.depth + 1
  FROM categories c
  INNER JOIN parent_chain pc ON c.uuid = pc.parent_uuid
  WHERE pc.depth < 100  -- Prevent infinite loop
)
SELECT COUNT(*) > 0 as has_circular_reference
FROM parent_chain
WHERE uuid = :categoryUuid;

-- Update position (within transaction)
START TRANSACTION;

-- Get current position
SELECT parent_uuid, position INTO @old_parent, @old_position
FROM categories
WHERE uuid = :categoryUuid AND language_uuid = :languageUuid;

-- Shift down categories in old position
UPDATE categories
SET position = position - 1,
    updated_at = NOW()
WHERE language_uuid = :languageUuid
  AND parent_uuid <=> @old_parent  -- Handle NULL comparison
  AND position > @old_position;

-- Shift up categories in new position
UPDATE categories
SET position = position + 1,
    updated_at = NOW()
WHERE language_uuid = :languageUuid
  AND parent_uuid <=> :newParentUuid
  AND position >= :newPosition
  AND uuid != :categoryUuid;

-- Update the moved category
UPDATE categories
SET parent_uuid = :newParentUuid,
    position = :newPosition,
    updated_at = NOW()
WHERE uuid = :categoryUuid
  AND language_uuid = :languageUuid;

COMMIT;
```

## Java/Spring Boot Example

```java
@PatchMapping("/languages/{languageUuid}/categories/{categoryUuid}/position")
public ResponseEntity<?> updateCategoryPosition(
    @PathVariable String languageUuid,
    @PathVariable String categoryUuid,
    @RequestBody UpdatePositionRequest request,
    @AuthenticationPrincipal UserDetails userDetails
) {
    try {
        // Validate user owns the language
        Language language = languageRepository.findById(languageUuid)
            .orElseThrow(() -> new ResourceNotFoundException("Language not found"));
        
        if (!language.getUserId().equals(userDetails.getUsername())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Validate category exists
        Category category = categoryRepository.findByUuidAndLanguageUuid(
            categoryUuid, languageUuid
        ).orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        // Validate parent if provided
        if (request.getParentUuid() != null) {
            Category parent = categoryRepository.findByUuidAndLanguageUuid(
                request.getParentUuid(), languageUuid
            ).orElseThrow(() -> new ResourceNotFoundException("Parent not found"));

            // Check circular reference
            if (categoryService.wouldCreateCircularReference(categoryUuid, request.getParentUuid())) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Cannot create circular reference"));
            }
        }

        // Update position
        categoryService.updatePosition(
            languageUuid,
            categoryUuid,
            request.getParentUuid(),
            request.getPosition()
        );

        return ResponseEntity.ok(new SuccessResponse("Category position updated"));

    } catch (ResourceNotFoundException e) {
        return ResponseEntity.notFound().build();
    } catch (Exception e) {
        log.error("Error updating category position", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("Internal server error"));
    }
}

@Data
public class UpdatePositionRequest {
    private String parentUuid;
    private Integer position;
}
```

## Python/FastAPI Example

```python
@router.patch("/languages/{language_uuid}/categories/{category_uuid}/position")
async def update_category_position(
    language_uuid: str,
    category_uuid: str,
    request: UpdatePositionRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    # Check language ownership
    language = db.query(Language).filter(
        Language.uuid == language_uuid,
        Language.user_id == current_user.id
    ).first()
    
    if not language:
        raise HTTPException(status_code=403, detail="Unauthorized")
    
    # Check category exists
    category = db.query(Category).filter(
        Category.uuid == category_uuid,
        Category.language_uuid == language_uuid
    ).first()
    
    if not category:
        raise HTTPException(status_code=404, detail="Category not found")
    
    # Validate parent
    if request.parent_uuid is not None:
        parent = db.query(Category).filter(
            Category.uuid == request.parent_uuid,
            Category.language_uuid == language_uuid
        ).first()
        
        if not parent:
            raise HTTPException(status_code=404, detail="Parent not found")
        
        # Check circular reference
        if is_circular_reference(db, category_uuid, request.parent_uuid):
            raise HTTPException(
                status_code=400, 
                detail="Cannot create circular reference"
            )
    
    # Update position
    try:
        update_category_position_service(
            db,
            language_uuid,
            category_uuid,
            request.parent_uuid,
            request.position
        )
        db.commit()
        return {"message": "Category position updated successfully"}
    except Exception as e:
        db.rollback()
        raise HTTPException(status_code=500, detail=str(e))

class UpdatePositionRequest(BaseModel):
    parent_uuid: Optional[str] = None
    position: int
```

## Testing the Endpoint

### Using cURL
```bash
curl -X PATCH \
  http://localhost:8080/languages/550e8400-e29b-41d4-a716-446655440000/categories/123e4567-e89b-12d3-a456-426614174000/position \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer YOUR_JWT_TOKEN' \
  -d '{
    "parentUuid": "789e0123-e89b-12d3-a456-426614174000",
    "position": 2
  }'
```

### Using Postman
```
Method: PATCH
URL: http://localhost:8080/languages/{languageUuid}/categories/{categoryUuid}/position
Headers:
  Content-Type: application/json
  Authorization: Bearer YOUR_JWT_TOKEN
Body (raw JSON):
{
  "parentUuid": "789e0123-e89b-12d3-a456-426614174000",
  "position": 2
}
```

### Test Cases
1. ✅ Move category to new parent
2. ✅ Move category to root (parentUuid: null)
3. ✅ Reorder within same parent
4. ❌ Try to create circular reference (should fail)
5. ❌ Try with non-existent parent (should fail)
6. ❌ Try without authorization (should fail)
7. ❌ Try with invalid position (negative number) (should fail)

