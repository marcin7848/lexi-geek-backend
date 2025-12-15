package io.learn.lexigeek.word.dto;

import java.time.LocalDate;
import java.util.UUID;

public interface WordStatsProjection {
    LocalDate getDate();

    UUID getLanguageUuid();

    Boolean getCorrect();

    Integer getCount();
}
