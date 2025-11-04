package io.learn.lexigeek.category.controller;

import io.learn.lexigeek.category.CategoryFacade;
import io.learn.lexigeek.category.dto.CategoryDto;
import io.learn.lexigeek.category.dto.CategoryFilterForm;
import io.learn.lexigeek.category.dto.CategoryForm;
import io.learn.lexigeek.common.pageable.PageDto;
import io.learn.lexigeek.common.pageable.PageableRequest;
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

import java.util.UUID;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@RequestMapping
class CategoryController {

    private static final class Routes {
        private static final String CATEGORIES = "/languages/{languageUuid}/categories";
        private static final String CATEGORY_BY_UUID = CATEGORIES + "/{uuid}";
    }

    private final CategoryFacade categoryFacade;

    @GetMapping(Routes.CATEGORIES)
    PageDto<CategoryDto> getCategories(@PathVariable final UUID languageUuid,
                                       @Valid final CategoryFilterForm form,
                                       @Valid final PageableRequest pageableRequest) {
        return categoryFacade.getCategories(languageUuid, form, pageableRequest);
    }

    @PostMapping(Routes.CATEGORIES)
    @ResponseStatus(HttpStatus.CREATED)
    void createCategory(@PathVariable final UUID languageUuid,
                        @RequestBody @Valid final CategoryForm form) {
        categoryFacade.createCategory(languageUuid, form);
    }

    @PutMapping(Routes.CATEGORY_BY_UUID)
    void editCategory(@PathVariable final UUID languageUuid,
                      @PathVariable final UUID uuid,
                      @RequestBody @Valid final CategoryForm form) {
        categoryFacade.editCategory(languageUuid, uuid, form);
    }

    @DeleteMapping(Routes.CATEGORY_BY_UUID)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteCategory(@PathVariable final UUID languageUuid,
                        @PathVariable final UUID uuid) {
        categoryFacade.deleteCategory(languageUuid, uuid);
    }
}

