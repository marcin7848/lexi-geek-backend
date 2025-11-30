
package io.learn.lexigeek.word;

import io.learn.lexigeek.word.dto.AutoTranslateForm;

import java.util.UUID;

public interface AutomaticTranslationFacade {

    void autoTranslate(final UUID languageUuid, final UUID categoryUuid, final AutoTranslateForm form);
}
