package io.learn.lexigeek.word.controller;

import io.learn.lexigeek.common.pageable.PageDto;
import io.learn.lexigeek.common.pageable.PageableRequest;
import io.learn.lexigeek.word.PublicWordFacade;
import io.learn.lexigeek.word.dto.PublicWordFilterForm;
import io.learn.lexigeek.word.dto.WordDto;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@RequestMapping
class PublicWordController {

    private static final class Routes {
        private static final String PUBLIC_WORDS = "/languages/{languageUuid}/categories/{categoryUuid}/public-words";
        private static final String PUBLIC_WORD_ACCEPT = PUBLIC_WORDS + "/{wordUuid}/accept";
        private static final String PUBLIC_WORD_REJECT = PUBLIC_WORDS + "/{wordUuid}/reject";
    }

    private final PublicWordFacade publicWordFacade;

    @GetMapping(Routes.PUBLIC_WORDS)
    PageDto<WordDto> getPublicWords(@PathVariable final UUID languageUuid,
                                    @PathVariable final UUID categoryUuid,
                                    @Valid final PublicWordFilterForm form,
                                    @Valid final PageableRequest pageableRequest) {
        return publicWordFacade.getPublicWords(languageUuid, categoryUuid, form, pageableRequest);
    }

    @PostMapping(Routes.PUBLIC_WORD_ACCEPT)
    WordDto acceptWord(@PathVariable final UUID languageUuid,
                       @PathVariable final UUID categoryUuid,
                       @PathVariable final UUID wordUuid) {
        return publicWordFacade.acceptWord(languageUuid, categoryUuid, wordUuid);
    }

    @PostMapping(Routes.PUBLIC_WORD_REJECT)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void rejectWord(@PathVariable final UUID languageUuid,
                    @PathVariable final UUID categoryUuid,
                    @PathVariable final UUID wordUuid) {
        publicWordFacade.rejectWord(languageUuid, categoryUuid, wordUuid);
    }
}
