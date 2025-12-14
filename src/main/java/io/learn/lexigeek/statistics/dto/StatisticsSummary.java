package io.learn.lexigeek.statistics.dto;

public record StatisticsSummary(
        Integer totalRepeatedWords,
        Integer totalAddedWords,
        Integer totalStars,
        AveragePerDay averagePerDay
) {
}

