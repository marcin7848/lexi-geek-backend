package io.learn.lexigeek.word.dto;

import io.learn.lexigeek.category.domain.CategoryMode;
import io.learn.lexigeek.word.domain.WordMechanism;

import java.util.List;
import java.util.UUID;

public record RepeatWordDto(
        UUID uuid,
        UUID wordUuid,
        String comment,
        WordMechanism mechanism,
        List<WordPartDto> wordParts,
        RepeatWordMethod method,
        CategoryMode categoryMode
) {
}

