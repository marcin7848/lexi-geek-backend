package io.learn.lexigeek.account;

import io.learn.lexigeek.account.dto.AccountDto;
import io.learn.lexigeek.account.dto.AccountForm;

public interface AccountFacade {

    AccountDto getLoggedAccount();

    void createAccount(final AccountForm form);
}
