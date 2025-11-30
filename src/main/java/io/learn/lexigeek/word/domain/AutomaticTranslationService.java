
package io.learn.lexigeek.word.domain;

import io.learn.lexigeek.category.CategoryFacade;
import io.learn.lexigeek.category.domain.CategoryMode;
import io.learn.lexigeek.category.dto.CategoryDto;
import io.learn.lexigeek.category.dto.CategoryFilterForm;
import io.learn.lexigeek.common.exception.NotFoundException;
import io.learn.lexigeek.common.pageable.PageDto;
import io.learn.lexigeek.common.pageable.PageableRequest;
import io.learn.lexigeek.common.pageable.PageableUtils;
import io.learn.lexigeek.common.pageable.SortOrder;
import io.learn.lexigeek.common.validation.ErrorCodes;
import io.learn.lexigeek.word.AutomaticTranslationFacade;
import io.learn.lexigeek.word.WordFacade;
import io.learn.lexigeek.word.dto.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static java.util.stream.Collectors.toSet;

@Service
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class AutomaticTranslationService implements AutomaticTranslationFacade {

    private final CategoryFacade categoryFacade;
    private final WordFacade wordFacade;

    @Override
    public void autoTranslate(final UUID languageUuid, final UUID categoryUuid, final AutoTranslateForm form) {
        categoryFacade.verifyCategoryAccess(languageUuid, categoryUuid);

        final List<AutomaticTranslationWord> translations = translate(form.method(), form.text());

    }

    private List<AutomaticTranslationWord> translate(final AutomaticTranslationMethod method, final String text){

    }
}
