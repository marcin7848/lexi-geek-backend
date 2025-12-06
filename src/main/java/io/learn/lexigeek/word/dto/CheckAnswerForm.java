package io.learn.lexigeek.word.dto;

import io.learn.lexigeek.word.domain.WordMethod;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record CheckAnswerForm(@NotNull Map<String, String> answers,
                              @NotNull WordMethod method) {
}
