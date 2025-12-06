package io.learn.lexigeek.word.dto;

public record CheckAnswerResultDto(
        Boolean correct,
        Integer wordsLeft,
        Boolean sessionActive
) {
}

