package io.learn.lexigeek.account;

import io.learn.lexigeek.account.dto.AccountDto;

public interface AccountFacade {

    AccountDto getAccountByEmail(final String email);
}
