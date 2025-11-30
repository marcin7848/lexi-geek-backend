package io.learn.lexigeek.word;

import io.learn.lexigeek.common.pageable.PageDto;
import io.learn.lexigeek.common.pageable.PageableRequest;
import io.learn.lexigeek.word.dto.PublicWordFilterForm;
import io.learn.lexigeek.word.dto.WordDto;

import java.util.UUID;

public interface PublicWordFacade {

    PageDto<WordDto> getPublicWords(final UUID languageUuid,
                                    final UUID categoryUuid,
                                    final PublicWordFilterForm form,
                                    final PageableRequest pageableRequest);

    WordDto acceptWord(final UUID languageUuid, final UUID categoryUuid, final UUID wordUuid);

    void rejectWord(final UUID languageUuid, final UUID categoryUuid, final UUID wordUuid);
}

