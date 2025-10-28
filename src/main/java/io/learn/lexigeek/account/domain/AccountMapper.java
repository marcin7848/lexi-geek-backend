package io.learn.lexigeek.account.domain;

import io.learn.lexigeek.account.dto.AccountDto;
import io.learn.lexigeek.account.dto.AccountForm;
import lombok.experimental.UtilityClass;

@UtilityClass
class AccountMapper {

    Account formToEntity(final AccountForm form) {
        final Account account = new Account();
        account.setUsername(form.username());
        account.setEmail(form.email());
        account.setPassword(form.password());
        return account;
    }

    AccountDto entityToDto(final Account account) {
        return new AccountDto(
                account.getUuid(),
                account.getUsername(),
                account.getEmail(),
                account.getPassword());
    }


}
