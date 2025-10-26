package io.learn.lexigeek.account.domain;

import io.learn.lexigeek.account.dto.AccountDto;
import lombok.experimental.UtilityClass;

@UtilityClass
class AccountMapper {

    AccountDto entityToDto(final Account account) {
        return new AccountDto(
                account.getUuid(),
                account.getUsername(),
                account.getEmail(),
                account.getPassword());
    }
}
