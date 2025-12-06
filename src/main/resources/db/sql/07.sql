--liquibase formatted sql
--changeset marcin.kaczor:7 labels:LG-8

CREATE TABLE repeat_sessions
(
    id          BIGSERIAL PRIMARY KEY,
    uuid        UUID        NOT NULL UNIQUE,
    method      VARCHAR(20) NOT NULL,
    created     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    language_id BIGINT      NOT NULL
        CONSTRAINT fk_repeat_session_language REFERENCES languages (id) ON DELETE CASCADE
);

CREATE INDEX idx_repeat_sessions_language ON repeat_sessions (language_id);

CREATE TABLE repeat_session_words
(
    repeat_session_id BIGINT NOT NULL
        CONSTRAINT fk_repeat_session_words_session REFERENCES repeat_sessions (id) ON DELETE CASCADE,
    word_id           BIGINT NOT NULL
        CONSTRAINT fk_repeat_session_words_word REFERENCES words (id) ON DELETE CASCADE
);

CREATE INDEX idx_repeat_session_words_session ON repeat_session_words (repeat_session_id);
CREATE INDEX idx_repeat_session_words_word ON repeat_session_words (word_id);
