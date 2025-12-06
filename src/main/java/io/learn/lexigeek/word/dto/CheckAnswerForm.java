package io.learn.lexigeek.word.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record CheckAnswerForm(@NotNull Map<String, String> answers) {
}
