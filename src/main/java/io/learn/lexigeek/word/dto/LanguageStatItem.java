package io.learn.lexigeek.word.dto;

import java.util.UUID;

public record LanguageStatItem(UUID languageUuid,
                               Integer count) {
}
