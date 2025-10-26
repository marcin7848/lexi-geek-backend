package io.learn.lexigeek.account.domain;

import io.learn.lexigeek.account.AccountFacade;
import io.learn.lexigeek.account.dto.AccountDto;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class AccountService implements AccountFacade {

    private static final class LogMessages {
        private static final String ACCOUNT_CREATED = "Account has been created";
    }

    private static final class ErrorMessages {
        private static final String ACCOUNT_NOT_FOUND = "Account with email: %s not found";
    }

    private final AccountRepository accountRepository;

    @Override
    public AccountDto getAccountByEmail(final String email) {
        final Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found for email: " + email));
        return AccountMapper.entityToDto(account);
    }
}
