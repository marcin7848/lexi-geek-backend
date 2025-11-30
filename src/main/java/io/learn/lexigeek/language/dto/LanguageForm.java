package io.learn.lexigeek.language.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LanguageForm(@NotNull @Size(min = 1, max = 20) String name,
                           @NotNull @Size(min = 1, max = 10) String shortcut,
                           @NotNull @Size(max = 10) String codeForSpeech,
                           @NotNull @Size(max = 10) String codeForTranslator,
                           @NotNull Boolean isPublic,
                           @NotNull @Size(max = 255) String specialLetters) {
}
