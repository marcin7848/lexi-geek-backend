package io.learn.lexigeek.word.controller;

import io.learn.lexigeek.common.pageable.PageDto;
import io.learn.lexigeek.common.pageable.PageableRequest;
import io.learn.lexigeek.word.WordFacade;
import io.learn.lexigeek.word.dto.UpdateWordCategoriesForm;
import io.learn.lexigeek.word.dto.WordDto;
import io.learn.lexigeek.word.dto.WordFilterForm;
import io.learn.lexigeek.word.dto.WordForm;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@RequestMapping
class WordController {

    private static final class Routes {
        private static final String WORDS = "/languages/{languageUuid}/categories/{categoryUuid}/words";
        private static final String WORD_BY_UUID = WORDS + "/{wordUuid}";
        private static final String WORD_ACCEPT = WORD_BY_UUID + "/accept";
        private static final String WORD_CHOOSE = WORD_BY_UUID + "/choose";
        private static final String WORD_CATEGORIES = "/languages/{languageUuid}/words/{wordUuid}/categories";
        private static final String RESET_TIME = "/languages/{languageUuid}/reset-time";
    }

    private final WordFacade wordFacade;

    @GetMapping(Routes.WORDS)
    PageDto<WordDto> getWords(@PathVariable final UUID languageUuid,
                              @PathVariable final UUID categoryUuid,
                              @Valid final WordFilterForm form,
                              @Valid final PageableRequest pageableRequest) {
        return wordFacade.getWords(languageUuid, categoryUuid, form, pageableRequest);
    }

    @GetMapping(Routes.WORD_BY_UUID)
    WordDto getWord(@PathVariable final UUID languageUuid,
                    @PathVariable final UUID categoryUuid,
                    @PathVariable final UUID wordUuid) {
        return wordFacade.getWord(languageUuid, categoryUuid, wordUuid);
    }

    @PostMapping(Routes.WORDS)
    @ResponseStatus(HttpStatus.CREATED)
    WordDto createWord(@PathVariable final UUID languageUuid,
                       @PathVariable final UUID categoryUuid,
                       @RequestBody @Valid final WordForm form) {
        return wordFacade.createWord(languageUuid, categoryUuid, form);
    }

    @PutMapping(Routes.WORD_BY_UUID)
    WordDto updateWord(@PathVariable final UUID languageUuid,
                       @PathVariable final UUID categoryUuid,
                       @PathVariable final UUID wordUuid,
                       @RequestBody @Valid final WordForm form) {
        return wordFacade.updateWord(languageUuid, categoryUuid, wordUuid, form);
    }

    @DeleteMapping(Routes.WORD_BY_UUID)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteWord(@PathVariable final UUID languageUuid,
                    @PathVariable final UUID categoryUuid,
                    @PathVariable final UUID wordUuid) {
        wordFacade.deleteWord(languageUuid, categoryUuid, wordUuid);
    }

    @PatchMapping(Routes.WORD_ACCEPT)
    WordDto acceptWord(@PathVariable final UUID languageUuid,
                       @PathVariable final UUID categoryUuid,
                       @PathVariable final UUID wordUuid) {
        return wordFacade.acceptWord(languageUuid, categoryUuid, wordUuid);
    }

    @PatchMapping(Routes.WORD_CHOOSE)
    WordDto chooseWord(@PathVariable final UUID languageUuid,
                       @PathVariable final UUID categoryUuid,
                       @PathVariable final UUID wordUuid) {
        return wordFacade.chooseWord(languageUuid, categoryUuid, wordUuid);
    }

    @PostMapping(Routes.WORD_CATEGORIES)
    WordDto updateWordCategories(@PathVariable final UUID languageUuid,
                                 @PathVariable final UUID wordUuid,
                                 @RequestBody @Valid final UpdateWordCategoriesForm form) {
        return wordFacade.updateWordCategories(languageUuid, wordUuid, form);
    }

    @PostMapping(Routes.RESET_TIME)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void resetWordTime(@PathVariable final UUID languageUuid,
                       @RequestParam(required = false) final UUID categoryUuid) {
        wordFacade.resetWordTime(languageUuid, categoryUuid);
    }
}

