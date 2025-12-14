package io.learn.lexigeek.account.domain;

import io.learn.lexigeek.account.AccountFacade;
import io.learn.lexigeek.account.dto.AccountDto;
import io.learn.lexigeek.account.dto.AccountForm;
import io.learn.lexigeek.account.dto.AccountStarsDto;
import io.learn.lexigeek.common.dto.DateRangeForm;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class AccountService implements AccountFacade {

    private static final class LogMessages {
        private static final String ACCOUNT_CREATED = "Account has been created with uuid {}";
    }

    private final AccountRepository accountRepository;
    private final AccountStarsRepository accountStarsRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public AccountDto getLoggedAccount() {
        final Account account = getAccount();
        return AccountMapper.entityToDto(account);
    }

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
        account.setPassword(passwordEncoder.encode(form.password()));
        accountRepository.save(account);
        log.info(LogMessages.ACCOUNT_CREATED, account.getUuid());
    }

    @Override
    @Transactional
    public void addStars(final AccountDto account, final Integer stars) {
        final Account accountEntity = getAccount(account.uuid());
        final AccountStars accountStars = new AccountStars();
        accountStars.setAccount(accountEntity);
        accountStars.setStars(stars);
        accountStars.setCreated(LocalDateTime.now());
        accountStarsRepository.save(accountStars);
    }

    @Override
    public Integer getStars() {
        final Account account = getAccount();
        return accountStarsRepository.getTotalStarsByAccount(account);
    }

    @Override
    public List<AccountStarsDto> getStars(final DateRangeForm range) {
        final Account account = getAccount();

        final Map<LocalDate, Integer> starsPerDay =
                accountStarsRepository.findByAccountAndCreatedBetweenOrderByCreatedAsc(
                                account, range.min().atStartOfDay(), range.max().atTime(LocalTime.MAX)).stream()
                        .collect(Collectors.groupingBy(
                                as -> as.getCreated().toLocalDate(),
                                Collectors.summingInt(AccountStars::getStars)
                        ));

        return range.min()
                .datesUntil(range.max().plusDays(1))
                .map(date -> AccountStarsMapper.toDto(
                        date,
                        starsPerDay.getOrDefault(date, 0)
                ))
                .toList();
    }

    private Account getAccount() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new AuthorizationException(ErrorCodes.UNAUTHORIZED);
        }

        final String email = authentication.getName();
        return getAccount(email);
    }

    private Account getAccount(final String email) {
        return accountRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.USER_NOT_FOUND, email));
    }

    private Account getAccount(final UUID uuid) {
        return accountRepository.findByUuid(uuid)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.USER_NOT_FOUND, uuid));
    }
}
