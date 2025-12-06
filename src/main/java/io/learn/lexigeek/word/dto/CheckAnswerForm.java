package io.learn.lexigeek.word.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record CheckAnswerForm(
        @NotNull(message = "Answers are required")
        Map<String, String> answers
) {
}

