package io.learn.lexigeek.word.dto;

import io.learn.lexigeek.word.domain.AutomaticTranslationMethod;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record AutoTranslateForm(@NotNull AutomaticTranslationMethod method,
                                @NotNull @NotEmpty String text) {
}
