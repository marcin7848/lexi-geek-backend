package io.learn.lexigeek.language.domain;

import io.learn.lexigeek.account.AccountFacade;
import io.learn.lexigeek.account.dto.AccountDto;
import io.learn.lexigeek.common.exception.NotFoundException;
import io.learn.lexigeek.common.pageable.PageDto;
import io.learn.lexigeek.common.pageable.PageableRequest;
import io.learn.lexigeek.common.pageable.PageableUtils;
import io.learn.lexigeek.common.pageable.SortOrder;
import io.learn.lexigeek.common.validation.ErrorCodes;
import io.learn.lexigeek.language.LanguageFacade;
import io.learn.lexigeek.language.dto.LanguageDto;
import io.learn.lexigeek.language.dto.LanguageFilterForm;
import io.learn.lexigeek.language.dto.LanguageForm;
import io.learn.lexigeek.language.dto.ShortcutDto;
import io.learn.lexigeek.task.TaskFacade;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class LanguageService implements LanguageFacade {

    private final LanguageRepository languageRepository;
    private final AccountRepository accountRepository;
    private final AccountFacade accountFacade;
    private final TaskFacade taskFacade;

    @Override
    public PageDto<LanguageDto> getLanguages(final LanguageFilterForm form, final PageableRequest pageableRequest) {
        pageableRequest.addDefaultSorts(new SortOrder(Language.Fields.shortcut, Sort.DEFAULT_DIRECTION));
        final AccountDto account = accountFacade.getLoggedAccount();

        final LanguageSpecification specification = new LanguageSpecification(form, account.id());

        return PageableUtils.toDto(languageRepository.findAll(specification, PageableUtils.createPageable(pageableRequest))
                .map(LanguageMapper::entityToDto), pageableRequest);
    }

    @Override
    @Transactional
    public void createLanguage(final LanguageForm form) {
        final AccountDto accountDto = accountFacade.getLoggedAccount();
        final Language language = LanguageMapper.formToEntity(form);
        final Account account = accountRepository.findById(accountDto.id())
                .orElseThrow(() -> new NotFoundException(ErrorCodes.USER_NOT_FOUND, accountDto.id()));
        language.setAccount(account);
        languageRepository.save(language);
        taskFacade.initializeTasksForLanguage(language.getUuid());
    }

    @Override
    public void editLanguage(final UUID uuid, final LanguageForm form) {
        final AccountDto account = accountFacade.getLoggedAccount();
        final Language language = languageRepository.findByUuidAndAccountId(uuid, account.id())
                .orElseThrow(() -> new NotFoundException(ErrorCodes.LANGUAGE_NOT_FOUND, uuid));
        LanguageMapper.updateEntityFromForm(language, form);
        languageRepository.save(language);
    }

    @Override
    @Transactional
    public void deleteLanguage(final UUID uuid) {
        final AccountDto account = accountFacade.getLoggedAccount();
        final Language language = languageRepository.findByUuidAndAccountId(uuid, account.id())
                .orElseThrow(() -> new NotFoundException(ErrorCodes.LANGUAGE_NOT_FOUND, uuid));
        languageRepository.delete(language);
    }

    @Override
    public List<ShortcutDto> getPopularShortcuts(final String shortcut) {
        return languageRepository.findPopularShortcuts(shortcut, PageRequest.of(0, 10));
    }

    @Override
    public void verifyLanguageOwnership(final UUID languageUuid) {
        final AccountDto account = accountFacade.getLoggedAccount();
        languageRepository.findByUuidAndAccountId(languageUuid, account.id())
                .orElseThrow(() -> new NotFoundException(ErrorCodes.LANGUAGE_NOT_FOUND, languageUuid));
    }
}
