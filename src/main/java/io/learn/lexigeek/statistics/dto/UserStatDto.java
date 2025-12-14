package io.learn.lexigeek.statistics.dto;

import java.util.Map;

public record UserStatDto(
        String date,
        Integer repeatDictionary,
        Integer repeatExercise,
        Integer addDictionary,
        Integer addExercise,
        Integer stars,
        Map<String, LanguageStats> languageBreakdown
) {
}

