package io.learn.lexigeek.account;

import io.learn.lexigeek.account.dto.AccountDto;
import io.learn.lexigeek.account.dto.AccountForm;
import io.learn.lexigeek.account.dto.AccountStarsDto;
import io.learn.lexigeek.common.dto.DateRangeForm;

import java.util.List;

public interface AccountFacade {

    AccountDto getLoggedAccount();

    AccountDto getAccountByEmail(final String email);

    void createAccount(final AccountForm form);

    void addStars(final Integer stars);

    Integer getStars();

    List<AccountStarsDto> getStars(final DateRangeForm range);
}
