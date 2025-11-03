package io.learn.lexigeek.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.LocalDate;

@Embeddable
public record DateRange(@Column(name = "min") LocalDate min,
                        @Column(name = "max") LocalDate max) {

    public static DateRange unlimited() {
        return new DateRange(LocalDate.MIN, LocalDate.MAX);
    }
}
