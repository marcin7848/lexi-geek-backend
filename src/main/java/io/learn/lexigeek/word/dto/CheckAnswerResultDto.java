package io.learn.lexigeek.word.dto;

import java.util.List;

public record CheckAnswerResultDto(Boolean correct,
                                   Integer wordsLeft,
                                   Boolean sessionActive,
                                   List<AnswerDetail> answerDetails) {

    public record AnswerDetail(String userAnswer,
                               String correctAnswer,
                               Boolean isCorrect) {
    }
}
