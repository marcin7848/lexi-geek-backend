package io.learn.lexigeek.statistics.dto;

import java.util.Map;

public record UserStatDto(String date,
                          Integer repeat,
                          Integer add,
                          Integer stars,
                          Map<String, LanguageStats> languageBreakdown) {
}
