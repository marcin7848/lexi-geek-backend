package io.learn.lexigeek.language.domain;

import io.learn.lexigeek.language.dto.LanguageDto;
import io.learn.lexigeek.language.dto.LanguageForm;
import lombok.experimental.UtilityClass;

@UtilityClass
class LanguageMapper {

    Language formToEntity(final LanguageForm form) {
        final Language language = new Language();
        updateEntityFromForm(language, form);
        return language;
    }

    void updateEntityFromForm(final Language language, final LanguageForm form) {
        language.setName(form.name());
        language.setShortcut(form.shortcut());
        language.setCodeForSpeech(form.codeForSpeech());
        language.setCodeForTranslator(form.codeForTranslator());
        language.setHidden(form.hidden());
        language.setSpecialLetters(form.specialLetters());
    }

    LanguageDto entityToDto(final Language language) {
        return new LanguageDto(
                language.getUuid(),
                language.getName(),
                language.getShortcut(),
                language.getCodeForSpeech(),
                language.getCodeForTranslator(),
                language.isHidden(),
                language.getSpecialLetters());
    }
}
