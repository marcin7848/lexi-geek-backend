package io.learn.lexigeek.account;

import io.learn.lexigeek.account.dto.AccountDto;
import io.learn.lexigeek.account.dto.AccountForm;

public interface AccountFacade {

    AccountDto getAccountByEmail(final String email);

    void createAccount(final AccountForm form);
}
