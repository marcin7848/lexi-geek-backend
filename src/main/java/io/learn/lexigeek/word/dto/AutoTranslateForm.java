package io.learn.lexigeek.word.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record AutoTranslateForm(@NotNull @NotEmpty String sourceLanguage,
                                @NotNull @NotEmpty String targetLanguage,
                                @NotNull @NotEmpty String text,
                                @NotNull SourcePart sourcePart) {
}
