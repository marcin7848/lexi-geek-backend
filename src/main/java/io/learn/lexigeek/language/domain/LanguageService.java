package io.learn.lexigeek.language.domain;

import io.learn.lexigeek.account.AccountFacade;
import io.learn.lexigeek.account.dto.AccountDto;
import io.learn.lexigeek.common.exception.NotFoundException;
import io.learn.lexigeek.common.validation.ErrorCodes;
import io.learn.lexigeek.language.LanguageFacade;
import io.learn.lexigeek.language.dto.LanguageDto;
import io.learn.lexigeek.language.dto.LanguageForm;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class LanguageService implements LanguageFacade {

    private final LanguageRepository languageRepository;
    private final AccountRepository accountRepository;
    private final AccountFacade accountFacade;

    @Override
    public List<LanguageDto> getLanguages() {
        final AccountDto account = accountFacade.getLoggedAccount();
        return languageRepository.findAllByAccountId(account.id()).stream()
                .map(LanguageMapper::entityToDto)
                .toList();
    }

    @Override
    public void createLanguage(final LanguageForm form) {
        final AccountDto accountDto = accountFacade.getLoggedAccount();
        final Language language = LanguageMapper.formToEntity(form);
        final Account account = accountRepository.findById(accountDto.id())
                .orElseThrow(() -> new NotFoundException(ErrorCodes.USER_NOT_FOUND, accountDto.id()));
        language.setAccount(account);
        languageRepository.save(language);
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
    public void deleteLanguage(final UUID uuid) {
        final AccountDto account = accountFacade.getLoggedAccount();
        final Language language = languageRepository.findByUuidAndAccountId(uuid, account.id())
                .orElseThrow(() -> new NotFoundException(ErrorCodes.LANGUAGE_NOT_FOUND, uuid));
        languageRepository.delete(language);
    }
}
