package io.learn.lexigeek.word.dto;

import io.learn.lexigeek.category.domain.CategoryMode;
import io.learn.lexigeek.word.domain.WordMechanism;
import io.learn.lexigeek.word.domain.WordMethod;

import java.util.List;
import java.util.UUID;

public record RepeatWordDto(UUID uuid,
                            String comment,
                            WordMechanism mechanism,
                            List<WordPartDto> wordParts,
                            WordMethod method,
                            CategoryMode categoryMode) {
}
