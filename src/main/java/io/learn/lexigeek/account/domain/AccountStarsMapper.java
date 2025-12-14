package io.learn.lexigeek.account.domain;

import io.learn.lexigeek.account.dto.AccountStarsDto;
import lombok.experimental.UtilityClass;

import java.time.LocalDate;

@UtilityClass
class AccountStarsMapper {

    AccountStarsDto entityToDto(final AccountStars accountStars) {
        return new AccountStarsDto(accountStars.getCreated().toLocalDate(), accountStars.getStars());
    }

    AccountStarsDto toDto(final LocalDate date, final Integer stars) {
        return new AccountStarsDto(date, stars);
    }
}
