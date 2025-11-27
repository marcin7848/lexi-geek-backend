package io.learn.lexigeek.word.dto;

import io.learn.lexigeek.word.domain.SeparatorType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record WordPartDto(
        UUID uuid,
        @NotNull Boolean answer,
        String basicWord,
        @NotNull Integer position,
        @NotNull Boolean toSpeech,
        @NotNull Boolean separator,
        SeparatorType separatorType,
        String word
) {
}

