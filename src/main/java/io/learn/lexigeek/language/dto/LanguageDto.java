package io.learn.lexigeek.language.dto;

import java.util.UUID;

public record LanguageDto(UUID uuid,
                          String name,
                          String shortcut,
                          String codeForSpeech,
                          boolean isPublic,
                          String specialLetters) {
}
