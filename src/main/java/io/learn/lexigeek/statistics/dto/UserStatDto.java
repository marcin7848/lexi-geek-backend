package io.learn.lexigeek.statistics.dto;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record UserStatDto(LocalDate date,
                          Integer repeat,
                          Integer add,
                          Integer stars,
                          Integer repeatErrors,
                          Map<UUID, LanguageStats> languageStats) {
}
