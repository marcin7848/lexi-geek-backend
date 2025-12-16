package io.learn.lexigeek.language.dto;

import java.util.UUID;

public record LanguageFilterForm(UUID uuid,
                                 String name,
                                 String shortcut,
                                 String codeForSpeech,
                                 Boolean isPublic,
                                 String specialLetters) {
}
