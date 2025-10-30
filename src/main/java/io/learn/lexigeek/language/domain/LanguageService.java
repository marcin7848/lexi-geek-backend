package io.learn.lexigeek.language.domain;

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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class LanguageService implements LanguageFacade {

    private final LanguageRepository languageRepository;

    @Override
    public List<LanguageDto> getLanguages() {
        return languageRepository.findAll().stream()
                .map(LanguageMapper::entityToDto)
                .collect(Collectors.toList());
    }

    @Override
    public void createLanguage(final LanguageForm form) {
        final Language language = LanguageMapper.formToEntity(form);
        languageRepository.save(language);
    }

    @Override
    public void editLanguage(final UUID uuid, final LanguageForm form) {
        final Language language = languageRepository.findByUuid(uuid)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.LANGUAGE_NOT_FOUND, uuid));
        LanguageMapper.updateEntityFromForm(language, form);
        languageRepository.save(language);
    }

    @Override
    public void deleteLanguage(final UUID uuid) {
        final Language language = languageRepository.findByUuid(uuid)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.LANGUAGE_NOT_FOUND, uuid));
        languageRepository.delete(language);
    }
}
