package io.learn.lexigeek.language;

import io.learn.lexigeek.language.dto.LanguageDto;
import io.learn.lexigeek.language.dto.LanguageForm;

import java.util.List;
import java.util.UUID;

public interface LanguageFacade {

    List<LanguageDto> getLanguages();

    void createLanguage(final LanguageForm form);

    void editLanguage(final UUID uuid, final LanguageForm form);

    void deleteLanguage(final UUID uuid);
}
