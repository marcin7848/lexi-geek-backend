package io.learn.lexigeek.word.controller;

import io.learn.lexigeek.word.AutomaticTranslationFacade;
import io.learn.lexigeek.word.dto.AutoTranslateForm;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@RequestMapping
class AutomaticTranslationController {

    private static final class Routes {
        private static final String AUTO_TRANSLATE = "/languages/{languageUuid}/categories/{categoryUuid}/auto-translate";
    }

    private final AutomaticTranslationFacade automaticTranslationFacade;

    @PostMapping(AutomaticTranslationController.Routes.AUTO_TRANSLATE)
    @ResponseStatus(HttpStatus.OK)
    void autoTranslate(@PathVariable final UUID languageUuid,
                       @PathVariable final UUID categoryUuid,
                       @RequestBody @Valid final AutoTranslateForm form) {
        automaticTranslationFacade.autoTranslate(languageUuid, categoryUuid, form);
    }
}
