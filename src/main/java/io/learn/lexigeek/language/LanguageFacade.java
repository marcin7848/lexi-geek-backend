package io.learn.lexigeek.language;

import io.learn.lexigeek.common.pageable.PageDto;
import io.learn.lexigeek.common.pageable.PageableRequest;
import io.learn.lexigeek.language.dto.LanguageDto;
import io.learn.lexigeek.language.dto.LanguageFilterForm;
import io.learn.lexigeek.language.dto.LanguageForm;
import io.learn.lexigeek.language.dto.ShortcutDto;

import java.util.List;
import java.util.UUID;

public interface LanguageFacade {

    PageDto<LanguageDto> getLanguages(final LanguageFilterForm form, final PageableRequest pageableRequest);

    void createLanguage(final LanguageForm form);

    void editLanguage(final UUID uuid, final LanguageForm form);

    void deleteLanguage(final UUID uuid);

    List<ShortcutDto> getPopularShortcuts(final String shortcutText);
}
