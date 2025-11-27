package io.learn.lexigeek.word;

import io.learn.lexigeek.common.pageable.PageDto;
import io.learn.lexigeek.common.pageable.PageableRequest;
import io.learn.lexigeek.word.dto.UpdateWordCategoriesForm;
import io.learn.lexigeek.word.dto.WordDto;
import io.learn.lexigeek.word.dto.WordFilterForm;
import io.learn.lexigeek.word.dto.WordForm;

import java.util.UUID;

public interface WordFacade {

    PageDto<WordDto> getWords(final UUID languageUuid, final UUID categoryUuid, final WordFilterForm form, final PageableRequest pageableRequest);

    WordDto getWord(final UUID languageUuid, final UUID categoryUuid, final UUID wordUuid);

    WordDto createWord(final UUID languageUuid, final UUID categoryUuid, final WordForm form);

    WordDto updateWord(final UUID languageUuid, final UUID categoryUuid, final UUID wordUuid, final WordForm form);

    void deleteWord(final UUID languageUuid, final UUID categoryUuid, final UUID wordUuid);

    WordDto acceptWord(final UUID languageUuid, final UUID categoryUuid, final UUID wordUuid);

    WordDto chooseWord(final UUID languageUuid, final UUID categoryUuid, final UUID wordUuid);

    WordDto updateWordCategories(final UUID languageUuid, final UUID wordUuid, final UpdateWordCategoriesForm form);
}
