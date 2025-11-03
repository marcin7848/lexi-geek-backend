package io.learn.lexigeek.language.domain;

import io.learn.lexigeek.account.AccountFacade;
import io.learn.lexigeek.account.dto.AccountDto;
import io.learn.lexigeek.common.pageable.PageDto;
import io.learn.lexigeek.common.pageable.PageableRequest;
import io.learn.lexigeek.common.pageable.OrderString;
import io.learn.lexigeek.language.dto.LanguageDto;
import io.learn.lexigeek.language.dto.LanguageFilterForm;
import io.learn.lexigeek.language.dto.LanguageForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LanguageServiceTest {

    private final LanguageRepository languageRepository = mock(LanguageRepository.class);
    private final AccountRepository accountRepository = mock(AccountRepository.class);
    private final AccountFacade accountFacade = mock(AccountFacade.class);
    private final LanguageService languageService = new LanguageService(languageRepository, accountRepository, accountFacade);;

    @Test
    void getLanguages_returnsPagedDtos() {
        final Long accountId = 1L;
        final UUID accountUuid = UUID.randomUUID();
        final AccountDto logged = new AccountDto(accountId, accountUuid, "user", "user@example.com", "secret");
        when(accountFacade.getLoggedAccount()).thenReturn(logged);

        final Language language = new Language();
        language.setUuid(UUID.randomUUID());
        language.setShortcut("EN");
        language.setName("English");

        when(languageRepository.findAll(any(LanguageSpecification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(language), PageRequest.of(0, 10), 1));

        final LanguageFilterForm filter = new LanguageFilterForm(null, null, null, null, null, null, null);
        final PageableRequest pageable = new PageableRequest(1, 10, null, OrderString.asc, false);

        final PageDto<LanguageDto> result = languageService.getLanguages(filter, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getItems()).hasSize(1);
        final LanguageDto dto = result.getItems().getFirst();
        assertThat(dto.shortcut()).isEqualTo("EN");
        assertThat(dto.name()).isEqualTo("English");
        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getPageSize()).isEqualTo(10);
    }

    @Test
    void createLanguage_savesEntityWithAccount() {
        final Long accountId = 1L;
        final UUID accountUuid = UUID.randomUUID();
        final AccountDto logged = new AccountDto(accountId, accountUuid, "user", "user@example.com", "secret");
        when(accountFacade.getLoggedAccount()).thenReturn(logged);

        final LanguageForm form = new LanguageForm("Polish", "PL", "pl-PL", "pl", false, "ąćęłńóśżź");

        final Account account = new Account();

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        languageService.createLanguage(form);

        ArgumentCaptor<Language> captor = ArgumentCaptor.forClass(Language.class);
        verify(languageRepository).save(captor.capture());
        final Language saved = captor.getValue();
        assertThat(saved.getAccount()).isEqualTo(account);
        assertThat(saved.getName()).isEqualTo("Polish");
        assertThat(saved.getShortcut()).isEqualTo("PL");
    }

    @Test
    void editLanguage_updatesAndSaves() {
        final Long accountId = 1L;
        final UUID accountUuid = UUID.randomUUID();
        final AccountDto logged = new AccountDto(accountId, accountUuid, "user", "user@example.com", "secret");
        when(accountFacade.getLoggedAccount()).thenReturn(logged);

        final UUID langUuid = UUID.randomUUID();
        final Language existing = new Language();
        existing.setUuid(langUuid);
        existing.setName("Old");
        existing.setShortcut("OL");

        when(languageRepository.findByUuidAndAccountId(langUuid, accountId)).thenReturn(Optional.of(existing));

        final LanguageForm form = new LanguageForm("New", "NW", "nw", "nw", false, "");

        languageService.editLanguage(langUuid, form);

        ArgumentCaptor<Language> captor = ArgumentCaptor.forClass(Language.class);
        verify(languageRepository).save(captor.capture());
        final Language saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("New");
        assertThat(saved.getShortcut()).isEqualTo("NW");
    }

    @Test
    void deleteLanguage_deletesExisting() {
        final Long accountId = 1L;
        final UUID accountUuid = UUID.randomUUID();
        final AccountDto logged = new AccountDto(accountId, accountUuid, "user", "user@example.com", "secret");
        when(accountFacade.getLoggedAccount()).thenReturn(logged);

        final UUID langUuid = UUID.randomUUID();
        final Language existing = new Language();
        existing.setUuid(langUuid);

        when(languageRepository.findByUuidAndAccountId(langUuid, accountId)).thenReturn(Optional.of(existing));

        languageService.deleteLanguage(langUuid);

        verify(languageRepository).delete(existing);
    }
}
