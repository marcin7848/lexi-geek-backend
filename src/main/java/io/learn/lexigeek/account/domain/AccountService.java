package io.learn.lexigeek.account.domain;

import io.learn.lexigeek.account.AccountFacade;
import io.learn.lexigeek.account.dto.AccountDto;
import io.learn.lexigeek.common.exception.AlreadyExistsException;
import io.learn.lexigeek.common.exception.NotFoundException;
import io.learn.lexigeek.common.validation.ErrorCodes;
import io.learn.lexigeek.account.dto.AccountForm;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class AccountService implements AccountFacade {

    private static final class LogMessages {
        private static final String ACCOUNT_CREATED = "Account has been created {}";
    }

    private static final class ErrorMessages {
        private static final String ACCOUNT_NOT_FOUND = "Account with email: %s not found";
    }

    private final AccountRepository accountRepository;

    @Override
    public AccountDto getAccountByEmail(final String email) {
        final Account account = getAccount(email);
        return AccountMapper.entityToDto(account);
    }

    @Override
    public void createAccount(final AccountForm form) {
        accountRepository.findByEmail(form.email()).ifPresent(a -> {
            throw new AlreadyExistsException(ErrorCodes.EMAIL_ALREADY_EXISTS, form.email());
        });
        final Account account = AccountMapper.formToEntity(form);
        accountRepository.save(account);
        log.info(LogMessages.ACCOUNT_CREATED, account);
    }

    private Account getAccount(final String email) {
        return accountRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.USER_NOT_FOUND, email));
    }
}
