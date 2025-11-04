package io.learn.lexigeek.language.controller;

import io.learn.lexigeek.common.pageable.PageDto;
import io.learn.lexigeek.common.pageable.PageableRequest;
import io.learn.lexigeek.language.LanguageFacade;
import io.learn.lexigeek.language.dto.LanguageDto;
import io.learn.lexigeek.language.dto.LanguageFilterForm;
import io.learn.lexigeek.language.dto.LanguageForm;
import io.learn.lexigeek.language.dto.ShortcutDto;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@RequestMapping
class LanguageController {

    private static final class Routes {
        private static final String LANGUAGES = "/languages";
        private static final String LANGUAGE_BY_UUID = LANGUAGES + "/{uuid}";
        private static final String SHORTCUTS = LANGUAGES + "/shortcuts";
    }

    private final LanguageFacade languageFacade;

    @GetMapping(Routes.LANGUAGES)
    PageDto<LanguageDto> getLanguages(@Valid final LanguageFilterForm form,
                                      @Valid final PageableRequest pageableRequest) {
        return languageFacade.getLanguages(form, pageableRequest);
    }

    @PostMapping(Routes.LANGUAGES)
    @ResponseStatus(HttpStatus.CREATED)
    void createLanguage(@RequestBody @Valid final LanguageForm form) {
        languageFacade.createLanguage(form);
    }

    @PutMapping(Routes.LANGUAGE_BY_UUID)
    void editLanguage(@PathVariable final UUID uuid,
                      @RequestBody @Valid final LanguageForm form) {
        languageFacade.editLanguage(uuid, form);
    }

    @DeleteMapping(Routes.LANGUAGE_BY_UUID)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteLanguage(@PathVariable final UUID uuid) {
        languageFacade.deleteLanguage(uuid);
    }

    @GetMapping(Routes.SHORTCUTS)
    List<ShortcutDto> getPopularShortcuts(final String shortcut) {
        return languageFacade.getPopularShortcuts(shortcut);
    }
}
