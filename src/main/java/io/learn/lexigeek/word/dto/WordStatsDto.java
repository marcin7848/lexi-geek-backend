package io.learn.lexigeek.word.dto;

import io.learn.lexigeek.word.domain.WordMethod;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record WordStatsDto(UUID uuid,
                           @NotNull Boolean correct,
                           @NotNull WordMethod method,
                           @NotNull LocalDateTime answerTime) {
}
