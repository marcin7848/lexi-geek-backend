package io.learn.lexigeek.word.dto;

import io.learn.lexigeek.category.domain.CategoryMethod;

import java.time.LocalDateTime;
import java.util.UUID;

public record RepeatSessionDto(UUID uuid,
                               UUID languageUuid,
                               Integer wordsLeft,
                               CategoryMethod method,
                               LocalDateTime created) {
}
