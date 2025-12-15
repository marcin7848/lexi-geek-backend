# Activity Controller API Documentation

## Overview
The Activity Controller provides endpoints to manage and retrieve user activities in the LexiGeek application. Activities track user interactions such as finishing repetitions and earning stars.

## Base URL
```
/activities
```

## Endpoints

### Get Activities

Retrieves a paginated list of activities with optional filtering.

#### Endpoint
```
GET /activities
```

#### Request Parameters

##### Filter Parameters (ActivityFilterForm)

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `languageUuid` | UUID | No | Filter activities by language UUID |
| `categoryUuid` | UUID | No | Filter activities by category UUID |
| `type` | ActivityType | No | Filter by activity type. Possible values: `REPEATING_FINISHED`, `STARS_ADDED` |
| `range.min` | LocalDateTime | No | Start date/time for filtering activities (format: `yyyy-MM-dd'T'HH:mm:ss`) |
| `range.max` | LocalDateTime | No | End date/time for filtering activities (format: `yyyy-MM-dd'T'HH:mm:ss`) |

##### Pagination Parameters (PageableRequest)

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `page` | Integer | No | 1 | Page number (minimum: 1) |
| `pageSize` | Integer | No | Default page size | Number of items per page (minimum: 1) |
| `sort` | String | No | - | Field name to sort by |
| `order` | OrderString | No | `asc` | Sort order. Possible values: `asc`, `desc` |
| `singlePage` | Boolean | No | `false` | Whether to return all results in a single page |

#### Response

Returns a paginated response containing activities.

**Status Code:** `200 OK`

**Response Body Structure:**

```json
{
  "page": 1,
  "pageSize": 20,
  "total": 100,
  "sort": "created",
  "order": "desc",
  "singlePage": false,
  "items": [
    {
      "uuid": "550e8400-e29b-41d4-a716-446655440000",
      "languageName": "English",
      "categoryName": "Vocabulary",
      "created": "2025-12-15T10:30:00",
      "type": "REPEATING_FINISHED",
      "param": "lesson-1"
    }
  ]
}
```

**Response Fields:**

| Field | Type | Description |
|-------|------|-------------|
| `page` | Integer | Current page number |
| `pageSize` | Integer | Number of items per page |
| `total` | Long | Total number of activities matching the filter |
| `sort` | String | Field used for sorting |
| `order` | OrderString | Sort order applied |
| `singlePage` | Boolean | Whether all results are in a single page |
| `items` | Array<ActivityDto> | Array of activity objects |

**ActivityDto Fields:**

| Field | Type | Description |
|-------|------|-------------|
| `uuid` | UUID | Unique identifier for the activity |
| `languageName` | String | Name of the language associated with the activity |
| `categoryName` | String | Name of the category associated with the activity |
| `created` | String | Date and time when the activity was created |
| `type` | String | Type of activity (`REPEATING_FINISHED` or `STARS_ADDED`) |
| `param` | String | Additional parameter or context for the activity |

#### Example Requests

##### Example 1: Get first page of all activities
```http
GET /activities?page=1&pageSize=20
```

##### Example 2: Get activities filtered by language
```http
GET /activities?languageUuid=550e8400-e29b-41d4-a716-446655440000&page=1&pageSize=10
```

##### Example 3: Get activities by type with date range
```http
GET /activities?type=REPEATING_FINISHED&range.min=2025-12-01T00:00:00&range.max=2025-12-31T23:59:59&page=1&pageSize=50
```

##### Example 4: Get activities sorted by creation date (descending)
```http
GET /activities?sort=created&order=desc&page=1&pageSize=20
```

##### Example 5: Get activities filtered by category and language
```http
GET /activities?languageUuid=550e8400-e29b-41d4-a716-446655440000&categoryUuid=660e8400-e29b-41d4-a716-446655440001&page=1&pageSize=15
```

#### Example Response

```json
{
  "page": 1,
  "pageSize": 20,
  "total": 150,
  "sort": "created",
  "order": "desc",
  "singlePage": false,
  "items": [
    {
      "uuid": "550e8400-e29b-41d4-a716-446655440000",
      "languageName": "English",
      "categoryName": "Vocabulary",
      "created": "2025-12-15T10:30:00",
      "type": "REPEATING_FINISHED",
      "param": "lesson-basic-1"
    },
    {
      "uuid": "550e8400-e29b-41d4-a716-446655440001",
      "languageName": "Spanish",
      "categoryName": "Grammar",
      "created": "2025-12-15T09:15:00",
      "type": "STARS_ADDED",
      "param": "achievement-first-star"
    },
    {
      "uuid": "550e8400-e29b-41d4-a716-446655440002",
      "languageName": "French",
      "categoryName": "Pronunciation",
      "created": "2025-12-14T18:45:00",
      "type": "REPEATING_FINISHED",
      "param": "lesson-pronunciation-3"
    }
  ]
}
```

## Error Responses

### Validation Error (400 Bad Request)

Returned when request parameters fail validation (e.g., page < 1, invalid UUID format).

```json
{
  "status": 400,
  "message": "Validation failed",
  "errors": [
    {
      "field": "page",
      "message": "must be greater than or equal to 1"
    }
  ]
}
```

### Internal Server Error (500)

Returned when an unexpected error occurs on the server.

```json
{
  "status": 500,
  "message": "An unexpected error occurred"
}
```

## Activity Types

| Type | Description |
|------|-------------|
| `REPEATING_FINISHED` | User completed a repetition/practice session |
| `STARS_ADDED` | User earned stars/points |

## Date/Time Format

All date/time parameters and responses use the ISO 8601 format:
```
yyyy-MM-dd'T'HH:mm:ss
```

Example: `2025-12-15T10:30:00`

## Notes

- All UUID parameters should be in the standard UUID format (e.g., `550e8400-e29b-41d4-a716-446655440000`)
- Pagination starts at page 1 (not 0)
- When `singlePage` is set to `true`, all matching results will be returned regardless of the `pageSize` parameter
- Date/time ranges are inclusive of both min and max values
- Multiple filter parameters can be combined for more specific queries
- The `param` field in ActivityDto contains context-specific information that varies by activity type

