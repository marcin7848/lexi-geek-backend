package io.learn.lexigeek.word.dto;

import java.time.LocalDate;
import java.util.List;

public record DateStatItem(LocalDate date,
                           List<LanguageStatItem> languageStats) {
}
