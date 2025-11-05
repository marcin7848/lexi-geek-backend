package io.learn.lexigeek.category.controller;

import io.learn.lexigeek.category.CategoryFacade;
import io.learn.lexigeek.category.dto.UpdateCategoryPositionForm;
import io.learn.lexigeek.common.exception.NotFoundException;
import io.learn.lexigeek.common.exception.ValidationException;
import io.learn.lexigeek.common.validation.ErrorCodes;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CategoryControllerDragDropTest {

    private final CategoryFacade categoryFacade = mock(CategoryFacade.class);
    private final CategoryController categoryController = new CategoryController(categoryFacade);

    @Test
    void shouldUpdateCategoryPosition_WhenValidRequest() {
        final UUID languageUuid = UUID.randomUUID();
        final UUID categoryUuid = UUID.randomUUID();
        final UUID parentUuid = UUID.randomUUID();

        final UpdateCategoryPositionForm form = new UpdateCategoryPositionForm(parentUuid, 0);

        doNothing().when(categoryFacade).updateCategoryPosition(languageUuid, categoryUuid, form);

        assertDoesNotThrow(() -> categoryController.updateCategoryPosition(languageUuid, categoryUuid, form));
        verify(categoryFacade).updateCategoryPosition(languageUuid, categoryUuid, form);
    }

    @Test
    void shouldUpdateCategoryPosition_ToRootLevel_WhenParentUuidIsNull() {
        final UUID languageUuid = UUID.randomUUID();
        final UUID categoryUuid = UUID.randomUUID();

        final UpdateCategoryPositionForm form = new UpdateCategoryPositionForm(null, 2);

        doNothing().when(categoryFacade).updateCategoryPosition(languageUuid, categoryUuid, form);

        assertDoesNotThrow(() -> categoryController.updateCategoryPosition(languageUuid, categoryUuid, form));
        verify(categoryFacade).updateCategoryPosition(languageUuid, categoryUuid, form);
    }

    @Test
    void shouldThrowNotFoundException_WhenCategoryDoesNotExist() {
        final UUID languageUuid = UUID.randomUUID();
        final UUID categoryUuid = UUID.randomUUID();

        final UpdateCategoryPositionForm form = new UpdateCategoryPositionForm(null, 0);

        doThrow(new NotFoundException(ErrorCodes.CATEGORY_NOT_FOUND, categoryUuid))
                .when(categoryFacade)
                .updateCategoryPosition(languageUuid, categoryUuid, form);

        assertThrows(NotFoundException.class,
                () -> categoryController.updateCategoryPosition(languageUuid, categoryUuid, form));
    }

    @Test
    void shouldThrowValidationException_WhenCircularReferenceDetected() {
        final UUID languageUuid = UUID.randomUUID();
        final UUID categoryUuid = UUID.randomUUID();
        final UUID parentUuid = UUID.randomUUID();

        final UpdateCategoryPositionForm form = new UpdateCategoryPositionForm(parentUuid, 0);

        doThrow(new ValidationException(ErrorCodes.CATEGORY_CIRCULAR_REFERENCE_ERROR))
                .when(categoryFacade)
                .updateCategoryPosition(languageUuid, categoryUuid, form);

        assertThrows(ValidationException.class,
                () -> categoryController.updateCategoryPosition(languageUuid, categoryUuid, form));
    }

    @Test
    void shouldThrowNotFoundException_WhenParentCategoryDoesNotExist() {
        final UUID languageUuid = UUID.randomUUID();
        final UUID categoryUuid = UUID.randomUUID();
        final UUID parentUuid = UUID.randomUUID();

        final UpdateCategoryPositionForm form = new UpdateCategoryPositionForm(parentUuid, 0);

        doThrow(new NotFoundException(ErrorCodes.PARENT_CATEGORY_NOT_FOUND, parentUuid))
                .when(categoryFacade)
                .updateCategoryPosition(languageUuid, categoryUuid, form);

        assertThrows(NotFoundException.class,
                () -> categoryController.updateCategoryPosition(languageUuid, categoryUuid, form));
    }

    @Test
    void shouldCallFacadeWithCorrectParameters() {
        final UUID languageUuid = UUID.randomUUID();
        final UUID categoryUuid = UUID.randomUUID();
        final UUID parentUuid = UUID.randomUUID();
        final Integer position = 5;

        final UpdateCategoryPositionForm form = new UpdateCategoryPositionForm(parentUuid, position);

        doNothing().when(categoryFacade).updateCategoryPosition(any(), any(), any());

        categoryController.updateCategoryPosition(languageUuid, categoryUuid, form);

        verify(categoryFacade).updateCategoryPosition(eq(languageUuid), eq(categoryUuid), eq(form));
    }
}
