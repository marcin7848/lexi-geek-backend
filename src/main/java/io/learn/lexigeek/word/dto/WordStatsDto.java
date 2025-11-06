package io.learn.lexigeek.word.dto;

import io.learn.lexigeek.word.domain.WordMethod;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record WordStatsDto(
        UUID uuid,
        @NotNull Integer answered,
        @NotNull Integer toAnswer,
        @NotNull WordMethod method
) {
}

