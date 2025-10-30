package io.learn.lexigeek.account.domain;

import io.learn.lexigeek.account.AccountFacade;
import io.learn.lexigeek.account.dto.AccountDto;
import io.learn.lexigeek.account.dto.AccountForm;
import io.learn.lexigeek.common.exception.AlreadyExistsException;
import io.learn.lexigeek.common.exception.AuthorizationException;
import io.learn.lexigeek.common.exception.NotFoundException;
import io.learn.lexigeek.common.validation.ErrorCodes;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class AccountService implements AccountFacade {

    private static final class LogMessages {
        private static final String ACCOUNT_CREATED = "Account has been created with uuid {}";
    }

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public AccountDto getLoggedAccount() {
        final Account account = getAccount();
        return AccountMapper.entityToDto(account);
    }

    @Override
    public void createAccount(final AccountForm form) {
        accountRepository.findByEmail(form.email()).ifPresent(a -> {
            throw new AlreadyExistsException(ErrorCodes.EMAIL_ALREADY_EXISTS, form.email());
        });

        final Account account = AccountMapper.formToEntity(form);
        account.setPassword(passwordEncoder.encode(form.password()));
        accountRepository.save(account);
        log.info(LogMessages.ACCOUNT_CREATED, account.getUuid());
    }

    private Account getAccount() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new AuthorizationException(ErrorCodes.UNAUTHORIZED);
        }

        final String email = authentication.getName();
        return accountRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.USER_NOT_FOUND, email));
    }
}
