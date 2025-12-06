package io.learn.lexigeek.word.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record RepeatSessionDto(
        UUID uuid,
        UUID languageUuid,
        Integer wordsLeft,
        RepeatMethod method,
        LocalDateTime created
) {
}

