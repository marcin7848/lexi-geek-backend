package io.learn.lexigeek.language.controller;

import io.learn.lexigeek.language.LanguageFacade;
import io.learn.lexigeek.language.dto.LanguageDto;
import io.learn.lexigeek.language.dto.LanguageForm;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@RequestMapping
class LanguageController {

    private static final class Routes {
        private static final String LANGUAGES = "/languages";
        private static final String LANGUAGE_BY_UUID = "/languages/{uuid}";
    }

    private final LanguageFacade languageFacade;

    @GetMapping(Routes.LANGUAGES)
    List<LanguageDto> getLanguages() {
        return languageFacade.getLanguages();
    }

    @PostMapping(Routes.LANGUAGES)
    @ResponseStatus(HttpStatus.CREATED)
    void createLanguage(@RequestBody @Valid final LanguageForm form) {
        languageFacade.createLanguage(form);
    }

    @PutMapping(Routes.LANGUAGE_BY_UUID)
    void editLanguage(@PathVariable("uuid") final UUID uuid, @RequestBody @Valid final LanguageForm form) {
        languageFacade.editLanguage(uuid, form);
    }

    @DeleteMapping(Routes.LANGUAGE_BY_UUID)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteLanguage(@PathVariable("uuid") final UUID uuid) {
        languageFacade.deleteLanguage(uuid);
    }
}
