package io.learn.lexigeek.account.dto;

import java.time.LocalDate;

public record AccountStarsDto(LocalDate date,
                              Integer stars
) {
}
