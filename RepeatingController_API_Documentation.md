# Repeating Controller API Documentation

## Overview
The Repeating Controller provides endpoints for managing word repetition sessions in the LexiGeek application. It allows users to start learning sessions, retrieve words for practice, check answers, and manage their active sessions.

**Base Path**: `/languages/{languageUuid}/repeat-session`

---

## Endpoints

### 1. Start Repeat Session

**POST** `/languages/{languageUuid}/repeat-session`

Creates a new repeat session for practicing words in a specific language.

#### Path Parameters
- `languageUuid` (UUID, required) - The unique identifier of the language

#### Request Body
```json
{
  "categoryUuids": ["uuid1", "uuid2"],
  "wordCount": 10,
  "method": "BOTH"
}
```

**StartRepeatSessionForm Fields:**
- `categoryUuids` (List<UUID>, required) - List of category UUIDs to include in the session. Must not be empty.
- `wordCount` (Integer, required) - Number of words to practice. Must be at least 1.
- `method` (RepeatMethod, required) - The repetition method to use

**RepeatMethod Options:**
- `BOTH` - Practice in both directions
- `QUESTION_TO_ANSWER` - Practice from question to answer only
- `ANSWER_TO_QUESTION` - Practice from answer to question only

#### Response
**Status Code:** `201 CREATED`

```json
{
  "uuid": "session-uuid",
  "languageUuid": "language-uuid",
  "wordsLeft": 10,
  "method": "BOTH",
  "created": "2025-12-06T10:30:00"
}
```

**RepeatSessionDto Fields:**
- `uuid` (UUID) - Unique identifier of the session
- `languageUuid` (UUID) - Language identifier
- `wordsLeft` (Integer) - Number of words remaining in the session
- `method` (RepeatMethod) - The repetition method being used
- `created` (LocalDateTime) - Timestamp when the session was created

---

### 2. Get Active Session

**GET** `/languages/{languageUuid}/repeat-session`

Retrieves the currently active repeat session for a specific language.

#### Path Parameters
- `languageUuid` (UUID, required) - The unique identifier of the language

#### Response
**Status Code:** `200 OK`

```json
{
  "uuid": "session-uuid",
  "languageUuid": "language-uuid",
  "wordsLeft": 7,
  "method": "BOTH",
  "created": "2025-12-06T10:30:00"
}
```

**RepeatSessionDto Fields:** Same as Start Repeat Session response

---

### 3. Get Next Word

**GET** `/languages/{languageUuid}/repeat-session/next-word`

Retrieves the next word to practice in the active session.

#### Path Parameters
- `languageUuid` (UUID, required) - The unique identifier of the language

#### Response
**Status Code:** `200 OK`

```json
{
  "uuid": "repeat-word-uuid",
  "wordUuid": "word-uuid",
  "comment": "Optional comment about the word",
  "mechanism": "FLASHCARD",
  "wordParts": [
    {
      "role": "QUESTION",
      "content": "Hello"
    },
    {
      "role": "ANSWER",
      "content": "Hola"
    }
  ],
  "method": "QUESTION_TO_ANSWER",
  "categoryMode": "INDIVIDUAL"
}
```

**RepeatWordDto Fields:**
- `uuid` (UUID) - Unique identifier for this repetition instance
- `wordUuid` (UUID) - The original word's unique identifier
- `comment` (String) - Optional comment or note about the word
- `mechanism` (WordMechanism) - The learning mechanism (e.g., FLASHCARD, TYPING)
- `wordParts` (List<WordPartDto>) - List of word parts (question/answer)
- `method` (RepeatWordMethod) - The method for this specific word
- `categoryMode` (CategoryMode) - The category mode (e.g., INDIVIDUAL, GROUP)

---

### 4. Check Answer

**POST** `/languages/{languageUuid}/repeat-session/words/{wordUuid}/check-answer`

Submits an answer for a word and validates it.

#### Path Parameters
- `languageUuid` (UUID, required) - The unique identifier of the language
- `wordUuid` (UUID, required) - The unique identifier of the word being answered

#### Request Body
```json
{
  "answers": {
    "part1": "user answer 1",
    "part2": "user answer 2"
  }
}
```

**CheckAnswerForm Fields:**
- `answers` (Map<String, String>, required) - Map of answer keys to user-provided answers

#### Response
**Status Code:** `200 OK`

```json
{
  "correct": true,
  "wordsLeft": 6,
  "sessionActive": true
}
```

**CheckAnswerResultDto Fields:**
- `correct` (Boolean) - Whether the answer was correct
- `wordsLeft` (Integer) - Number of words remaining in the session
- `sessionActive` (Boolean) - Whether the session is still active

---

### 5. Reset Session

**DELETE** `/languages/{languageUuid}/repeat-session`

Deletes/resets the current active session for a specific language.

#### Path Parameters
- `languageUuid` (UUID, required) - The unique identifier of the language

#### Response
**Status Code:** `204 NO CONTENT`

No response body.

---

## Error Responses

All endpoints may return the following error responses:

### 400 Bad Request
Returned when validation fails (e.g., invalid request body, missing required fields)

### 404 Not Found
Returned when the specified language or session is not found

### 500 Internal Server Error
Returned when an unexpected server error occurs

---

## Usage Flow

1. **Start a session** using `POST /languages/{languageUuid}/repeat-session`
2. **Get the next word** using `GET /languages/{languageUuid}/repeat-session/next-word`
3. **Submit answer** using `POST /languages/{languageUuid}/repeat-session/words/{wordUuid}/check-answer`
4. **Repeat steps 2-3** until no words are left (`wordsLeft` = 0)
5. **Optionally reset** the session using `DELETE /languages/{languageUuid}/repeat-session`

You can also retrieve the active session at any time using `GET /languages/{languageUuid}/repeat-session` to check progress.

---

## Notes

- Only one active session per language is allowed at a time
- Sessions track progress and remaining words automatically
- Word selection is based on the specified categories and word count
- The `method` parameter determines how words are presented (question→answer, answer→question, or both)

