package io.learn.lexigeek.category.domain;

import io.learn.lexigeek.category.dto.CategoryDto;
import io.learn.lexigeek.category.dto.CategoryFilterForm;
import io.learn.lexigeek.category.dto.CategoryForm;
import io.learn.lexigeek.category.dto.UpdateCategoryPositionForm;
import io.learn.lexigeek.common.exception.NotFoundException;
import io.learn.lexigeek.common.exception.ValidationException;
import io.learn.lexigeek.common.pageable.OrderString;
import io.learn.lexigeek.common.pageable.PageDto;
import io.learn.lexigeek.common.pageable.PageableRequest;
import io.learn.lexigeek.common.validation.ErrorCodes;
import io.learn.lexigeek.language.LanguageFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CategoryServiceTest {

    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final LanguageRepository languageRepository = mock(LanguageRepository.class);
    private final LanguageFacade languageFacade = mock(LanguageFacade.class);
    private final CategoryService categoryService = new CategoryService(categoryRepository, languageRepository, languageFacade);

    private UUID languageUuid;
    private Language language;

    @BeforeEach
    void setUp() {
        languageUuid = UUID.randomUUID();
        language = new Language();
        language.setUuid(languageUuid);
    }

    @Nested
    class GetCategoriesTests {

        @Test
        void success_returnsPagedDtos() {
            // Given
            doNothing().when(languageFacade).verifyLanguageOwnership(languageUuid);

            final Category category = new Category();
            category.setUuid(UUID.randomUUID());
            category.setName("Grammar");
            category.setPosition(0);
            category.setMode(CategoryMode.DICTIONARY);
            category.setMethod(CategoryMethod.QUESTION_TO_ANSWER);

            when(categoryRepository.findAll(any(CategorySpecification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.singletonList(category), PageRequest.of(0, 10), 1));

            final CategoryFilterForm filter = new CategoryFilterForm(null, null, null, null, null, null);
            final PageableRequest pageable = new PageableRequest(1, 10, null, OrderString.asc, false);

            // When
            final PageDto<CategoryDto> result = categoryService.getCategories(languageUuid, filter, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getItems()).hasSize(1);
            final CategoryDto dto = result.getItems().getFirst();
            assertThat(dto.name()).isEqualTo("Grammar");
            assertThat(dto.position()).isEqualTo(0);
            assertThat(result.getPage()).isEqualTo(1);
            assertThat(result.getPageSize()).isEqualTo(10);

            verify(languageFacade).verifyLanguageOwnership(languageUuid);
        }

        @Test
        void emptyResult_returnsEmptyPage() {
            // Given
            doNothing().when(languageFacade).verifyLanguageOwnership(languageUuid);

            when(categoryRepository.findAll(any(CategorySpecification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0));

            final CategoryFilterForm filter = new CategoryFilterForm(null, null, null, null, null, null);
            final PageableRequest pageable = new PageableRequest(1, 10, null, OrderString.asc, false);

            // When
            final PageDto<CategoryDto> result = categoryService.getCategories(languageUuid, filter, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getItems()).isEmpty();
            assertThat(result.getPage()).isEqualTo(1);
            assertThat(result.getPageSize()).isEqualTo(10);
        }

        @Test
        void whenLanguageOwnershipVerificationFails_throwsException() {
            // Given
            doThrow(new NotFoundException(ErrorCodes.LANGUAGE_NOT_FOUND, languageUuid))
                    .when(languageFacade).verifyLanguageOwnership(languageUuid);

            final CategoryFilterForm filter = new CategoryFilterForm(null, null, null, null, null, null);
            final PageableRequest pageable = new PageableRequest(1, 10, null, OrderString.asc, false);

            // When & Then
            assertThrows(NotFoundException.class,
                    () -> categoryService.getCategories(languageUuid, filter, pageable));

            verify(categoryRepository, never()).findAll(any(CategorySpecification.class), any(Pageable.class));
        }
    }

    @Nested
    class CreateCategoryTests {

        @Test
        void success_savesEntityWithLanguageAndCorrectPosition() {
            // Given
            doNothing().when(languageFacade).verifyLanguageOwnership(languageUuid);
            when(languageRepository.findByUuid(languageUuid)).thenReturn(Optional.of(language));
            when(categoryRepository.findMaxPositionByLanguageUuid(languageUuid)).thenReturn(2);

            final CategoryForm form = new CategoryForm(
                    "Vocabulary",
                    CategoryMode.DICTIONARY,
                    CategoryMethod.BOTH,
                    null
            );

            // When
            categoryService.createCategory(languageUuid, form);

            // Then
            final ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(captor.capture());
            final Category saved = captor.getValue();
            assertThat(saved.getLanguage()).isEqualTo(language);
            assertThat(saved.getName()).isEqualTo("Vocabulary");
            assertThat(saved.getPosition()).isEqualTo(3); // maxPosition + 1
            assertThat(saved.getParent()).isNull();
            assertThat(saved.getMode()).isEqualTo(CategoryMode.DICTIONARY);
            assertThat(saved.getMethod()).isEqualTo(CategoryMethod.BOTH);
        }

        @Test
        void success_withParent_savesEntityWithParent() {
            // Given
            final UUID parentUuid = UUID.randomUUID();
            final Category parent = new Category();
            parent.setUuid(parentUuid);
            parent.setName("Parent");

            doNothing().when(languageFacade).verifyLanguageOwnership(languageUuid);
            when(languageRepository.findByUuid(languageUuid)).thenReturn(Optional.of(language));
            when(categoryRepository.findMaxPositionByLanguageUuid(languageUuid)).thenReturn(-1);
            when(categoryRepository.findByUuidAndLanguageUuid(parentUuid, languageUuid))
                    .thenReturn(Optional.of(parent));

            final CategoryForm form = new CategoryForm(
                    "Child Category",
                    CategoryMode.EXERCISE,
                    CategoryMethod.ANSWER_TO_QUESTION,
                    parentUuid
            );

            // When
            categoryService.createCategory(languageUuid, form);

            // Then
            final ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(captor.capture());
            final Category saved = captor.getValue();
            assertThat(saved.getParent()).isEqualTo(parent);
            assertThat(saved.getName()).isEqualTo("Child Category");
            assertThat(saved.getPosition()).isEqualTo(0);
        }

        @Test
        void whenLanguageNotFound_throwsNotFoundException_andDoesNotSave() {
            // Given
            doNothing().when(languageFacade).verifyLanguageOwnership(languageUuid);
            when(languageRepository.findByUuid(languageUuid)).thenReturn(Optional.empty());

            final CategoryForm form = new CategoryForm(
                    "Category",
                    CategoryMode.DICTIONARY,
                    CategoryMethod.QUESTION_TO_ANSWER,
                    null
            );

            // When & Then
            assertThatThrownBy(() -> categoryService.createCategory(languageUuid, form))
                    .isInstanceOf(NotFoundException.class)
                    .hasFieldOrPropertyWithValue("error", ErrorCodes.LANGUAGE_NOT_FOUND);

            verify(categoryRepository, never()).save(any());
        }

        @Test
        void whenParentNotFound_throwsNotFoundException_andDoesNotSave() {
            // Given
            final UUID parentUuid = UUID.randomUUID();

            doNothing().when(languageFacade).verifyLanguageOwnership(languageUuid);
            when(languageRepository.findByUuid(languageUuid)).thenReturn(Optional.of(language));
            when(categoryRepository.findMaxPositionByLanguageUuid(languageUuid)).thenReturn(0);
            when(categoryRepository.findByUuidAndLanguageUuid(parentUuid, languageUuid))
                    .thenReturn(Optional.empty());

            final CategoryForm form = new CategoryForm(
                    "Child",
                    CategoryMode.DICTIONARY,
                    CategoryMethod.QUESTION_TO_ANSWER,
                    parentUuid
            );

            // When & Then
            assertThatThrownBy(() -> categoryService.createCategory(languageUuid, form))
                    .isInstanceOf(NotFoundException.class)
                    .hasFieldOrPropertyWithValue("error", ErrorCodes.PARENT_CATEGORY_NOT_FOUND);

            verify(categoryRepository, never()).save(any());
        }

        @Test
        void whenLanguageOwnershipVerificationFails_throwsException_andDoesNotSave() {
            // Given
            doThrow(new NotFoundException(ErrorCodes.LANGUAGE_NOT_FOUND, languageUuid))
                    .when(languageFacade).verifyLanguageOwnership(languageUuid);

            final CategoryForm form = new CategoryForm(
                    "Category",
                    CategoryMode.DICTIONARY,
                    CategoryMethod.QUESTION_TO_ANSWER,
                    null
            );

            // When & Then
            assertThrows(NotFoundException.class,
                    () -> categoryService.createCategory(languageUuid, form));

            verify(categoryRepository, never()).save(any());
        }
    }

    @Nested
    class EditCategoryTests {

        private UUID categoryUuid;
        private Category existingCategory;

        @BeforeEach
        void setUp() {
            categoryUuid = UUID.randomUUID();
            existingCategory = new Category();
            existingCategory.setUuid(categoryUuid);
            existingCategory.setName("Old Name");
            existingCategory.setMode(CategoryMode.DICTIONARY);
            existingCategory.setMethod(CategoryMethod.QUESTION_TO_ANSWER);
            existingCategory.setPosition(1);
        }

        @Test
        void success_updatesAndSaves() {
            // Given
            doNothing().when(languageFacade).verifyLanguageOwnership(languageUuid);
            when(categoryRepository.findByUuidAndLanguageUuid(categoryUuid, languageUuid))
                    .thenReturn(Optional.of(existingCategory));

            final CategoryForm form = new CategoryForm(
                    "New Name",
                    CategoryMode.EXERCISE,
                    CategoryMethod.BOTH,
                    null
            );

            // When
            categoryService.editCategory(languageUuid, categoryUuid, form);

            // Then
            final ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(captor.capture());
            final Category saved = captor.getValue();
            assertThat(saved.getName()).isEqualTo("New Name");
            assertThat(saved.getMode()).isEqualTo(CategoryMode.EXERCISE);
            assertThat(saved.getMethod()).isEqualTo(CategoryMethod.BOTH);
            assertThat(saved.getParent()).isNull();
        }

        @Test
        void success_withNewParent_updatesParent() {
            // Given
            final UUID newParentUuid = UUID.randomUUID();
            final Category newParent = new Category();
            newParent.setUuid(newParentUuid);

            doNothing().when(languageFacade).verifyLanguageOwnership(languageUuid);
            when(categoryRepository.findByUuidAndLanguageUuid(categoryUuid, languageUuid))
                    .thenReturn(Optional.of(existingCategory));
            when(categoryRepository.findByUuidAndLanguageUuid(newParentUuid, languageUuid))
                    .thenReturn(Optional.of(newParent));

            final CategoryForm form = new CategoryForm(
                    "Updated",
                    CategoryMode.DICTIONARY,
                    CategoryMethod.QUESTION_TO_ANSWER,
                    newParentUuid
            );

            // When
            categoryService.editCategory(languageUuid, categoryUuid, form);

            // Then
            final ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(captor.capture());
            final Category saved = captor.getValue();
            assertThat(saved.getParent()).isEqualTo(newParent);
        }

        @Test
        void whenCategoryNotFound_throwsNotFoundException_andDoesNotSave() {
            // Given
            doNothing().when(languageFacade).verifyLanguageOwnership(languageUuid);
            when(categoryRepository.findByUuidAndLanguageUuid(categoryUuid, languageUuid))
                    .thenReturn(Optional.empty());

            final CategoryForm form = new CategoryForm(
                    "New Name",
                    CategoryMode.DICTIONARY,
                    CategoryMethod.QUESTION_TO_ANSWER,
                    null
            );

            // When & Then
            assertThatThrownBy(() -> categoryService.editCategory(languageUuid, categoryUuid, form))
                    .isInstanceOf(NotFoundException.class)
                    .hasFieldOrPropertyWithValue("error", ErrorCodes.CATEGORY_NOT_FOUND);

            verify(categoryRepository, never()).save(any());
        }

        @Test
        void whenNewParentNotFound_throwsNotFoundException_andDoesNotSave() {
            // Given
            final UUID newParentUuid = UUID.randomUUID();

            doNothing().when(languageFacade).verifyLanguageOwnership(languageUuid);
            when(categoryRepository.findByUuidAndLanguageUuid(categoryUuid, languageUuid))
                    .thenReturn(Optional.of(existingCategory));
            when(categoryRepository.findByUuidAndLanguageUuid(newParentUuid, languageUuid))
                    .thenReturn(Optional.empty());

            final CategoryForm form = new CategoryForm(
                    "Updated",
                    CategoryMode.DICTIONARY,
                    CategoryMethod.QUESTION_TO_ANSWER,
                    newParentUuid
            );

            // When & Then
            assertThatThrownBy(() -> categoryService.editCategory(languageUuid, categoryUuid, form))
                    .isInstanceOf(NotFoundException.class)
                    .hasFieldOrPropertyWithValue("error", ErrorCodes.PARENT_CATEGORY_NOT_FOUND);

            verify(categoryRepository, never()).save(any());
        }
    }

    @Nested
    class DeleteCategoryTests {

        private UUID categoryUuid;
        private Category existingCategory;

        @BeforeEach
        void setUp() {
            categoryUuid = UUID.randomUUID();
            existingCategory = new Category();
            existingCategory.setUuid(categoryUuid);
            existingCategory.setName("Category to delete");
        }

        @Test
        void success_deletesExisting() {
            // Given
            doNothing().when(languageFacade).verifyLanguageOwnership(languageUuid);
            when(categoryRepository.findByUuidAndLanguageUuid(categoryUuid, languageUuid))
                    .thenReturn(Optional.of(existingCategory));

            // When
            categoryService.deleteCategory(languageUuid, categoryUuid);

            // Then
            verify(categoryRepository).delete(existingCategory);
        }

        @Test
        void whenCategoryNotFound_throwsNotFoundException_andDoesNotDelete() {
            // Given
            doNothing().when(languageFacade).verifyLanguageOwnership(languageUuid);
            when(categoryRepository.findByUuidAndLanguageUuid(categoryUuid, languageUuid))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> categoryService.deleteCategory(languageUuid, categoryUuid))
                    .isInstanceOf(NotFoundException.class)
                    .hasFieldOrPropertyWithValue("error", ErrorCodes.CATEGORY_NOT_FOUND);

            verify(categoryRepository, never()).delete(any(Category.class));
        }

        @Test
        void whenLanguageOwnershipVerificationFails_throwsException_andDoesNotDelete() {
            // Given
            doThrow(new NotFoundException(ErrorCodes.LANGUAGE_NOT_FOUND, languageUuid))
                    .when(languageFacade).verifyLanguageOwnership(languageUuid);

            // When & Then
            assertThrows(NotFoundException.class,
                    () -> categoryService.deleteCategory(languageUuid, categoryUuid));

            verify(categoryRepository, never()).delete(any(Category.class));
        }
    }

    @Nested
    class UpdateCategoryPositionTests {

        private UUID categoryUuid;
        private Category existingCategory;

        @BeforeEach
        void setUp() {
            categoryUuid = UUID.randomUUID();
            existingCategory = new Category();
            existingCategory.setUuid(categoryUuid);
            existingCategory.setName("Category");
            existingCategory.setPosition(2);
            existingCategory.setParent(null);
        }

        @Test
        void success_movingUp_incrementsPositionsBetween() {
            // Given
            doNothing().when(languageFacade).verifyLanguageOwnership(languageUuid);
            when(categoryRepository.findByUuidAndLanguageUuid(categoryUuid, languageUuid))
                    .thenReturn(Optional.of(existingCategory));

            final UpdateCategoryPositionForm form = new UpdateCategoryPositionForm(null, 0);

            // When
            categoryService.updateCategoryPosition(languageUuid, categoryUuid, form);

            // Then
            verify(categoryRepository).incrementPositionsBetween(languageUuid, 0, 2, categoryUuid);
            verify(categoryRepository, never()).decrementPositionsBetween(any(), any(), any(), any());

            final ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(captor.capture());
            final Category saved = captor.getValue();
            assertThat(saved.getPosition()).isEqualTo(0);
            assertThat(saved.getParent()).isNull();
        }

        @Test
        void success_movingDown_decrementsPositionsBetween() {
            // Given
            existingCategory.setPosition(1);

            doNothing().when(languageFacade).verifyLanguageOwnership(languageUuid);
            when(categoryRepository.findByUuidAndLanguageUuid(categoryUuid, languageUuid))
                    .thenReturn(Optional.of(existingCategory));

            final UpdateCategoryPositionForm form = new UpdateCategoryPositionForm(null, 5);

            // When
            categoryService.updateCategoryPosition(languageUuid, categoryUuid, form);

            // Then
            verify(categoryRepository).decrementPositionsBetween(languageUuid, 1, 5, categoryUuid);
            verify(categoryRepository, never()).incrementPositionsBetween(any(), any(), any(), any());

            final ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(captor.capture());
            final Category saved = captor.getValue();
            assertThat(saved.getPosition()).isEqualTo(5);
        }

        @Test
        void success_samePosition_onlySaves() {
            // Given
            existingCategory.setPosition(3);

            doNothing().when(languageFacade).verifyLanguageOwnership(languageUuid);
            when(categoryRepository.findByUuidAndLanguageUuid(categoryUuid, languageUuid))
                    .thenReturn(Optional.of(existingCategory));

            final UpdateCategoryPositionForm form = new UpdateCategoryPositionForm(null, 3);

            // When
            categoryService.updateCategoryPosition(languageUuid, categoryUuid, form);

            // Then
            verify(categoryRepository, never()).incrementPositionsBetween(any(), any(), any(), any());
            verify(categoryRepository, never()).decrementPositionsBetween(any(), any(), any(), any());
            verify(categoryRepository).save(existingCategory);
        }

        @Test
        void success_withNewParent_updatesParent() {
            // Given
            final UUID newParentUuid = UUID.randomUUID();
            final Category newParent = new Category();
            newParent.setUuid(newParentUuid);

            doNothing().when(languageFacade).verifyLanguageOwnership(languageUuid);
            when(categoryRepository.findByUuidAndLanguageUuid(categoryUuid, languageUuid))
                    .thenReturn(Optional.of(existingCategory));
            when(categoryRepository.findByUuidAndLanguageUuid(newParentUuid, languageUuid))
                    .thenReturn(Optional.of(newParent));
            when(categoryRepository.isInParentHierarchy(anyString(), anyString())).thenReturn(false);

            final UpdateCategoryPositionForm form = new UpdateCategoryPositionForm(newParentUuid, 1);

            // When
            categoryService.updateCategoryPosition(languageUuid, categoryUuid, form);

            // Then
            final ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(captor.capture());
            final Category saved = captor.getValue();
            assertThat(saved.getParent()).isEqualTo(newParent);
            assertThat(saved.getPosition()).isEqualTo(1);
        }

        @Test
        void whenCategoryNotFound_throwsNotFoundException() {
            // Given
            doNothing().when(languageFacade).verifyLanguageOwnership(languageUuid);
            when(categoryRepository.findByUuidAndLanguageUuid(categoryUuid, languageUuid))
                    .thenReturn(Optional.empty());

            final UpdateCategoryPositionForm form = new UpdateCategoryPositionForm(null, 0);

            // When & Then
            assertThatThrownBy(() -> categoryService.updateCategoryPosition(languageUuid, categoryUuid, form))
                    .isInstanceOf(NotFoundException.class)
                    .hasFieldOrPropertyWithValue("error", ErrorCodes.CATEGORY_NOT_FOUND);

            verify(categoryRepository, never()).save(any());
        }

        @Test
        void whenParentNotFound_throwsNotFoundException() {
            // Given
            final UUID newParentUuid = UUID.randomUUID();

            doNothing().when(languageFacade).verifyLanguageOwnership(languageUuid);
            when(categoryRepository.findByUuidAndLanguageUuid(categoryUuid, languageUuid))
                    .thenReturn(Optional.of(existingCategory));
            when(categoryRepository.findByUuidAndLanguageUuid(newParentUuid, languageUuid))
                    .thenReturn(Optional.empty());

            final UpdateCategoryPositionForm form = new UpdateCategoryPositionForm(newParentUuid, 0);

            // When & Then
            assertThatThrownBy(() -> categoryService.updateCategoryPosition(languageUuid, categoryUuid, form))
                    .isInstanceOf(NotFoundException.class)
                    .hasFieldOrPropertyWithValue("error", ErrorCodes.PARENT_CATEGORY_NOT_FOUND);

            verify(categoryRepository, never()).save(any());
        }

        @Test
        void whenCircularReference_selfReference_throwsValidationException() {
            // Given
            doNothing().when(languageFacade).verifyLanguageOwnership(languageUuid);
            when(categoryRepository.findByUuidAndLanguageUuid(categoryUuid, languageUuid))
                    .thenReturn(Optional.of(existingCategory));
            when(categoryRepository.findByUuidAndLanguageUuid(categoryUuid, languageUuid))
                    .thenReturn(Optional.of(existingCategory));

            final UpdateCategoryPositionForm form = new UpdateCategoryPositionForm(categoryUuid, 0);

            // When & Then
            assertThatThrownBy(() -> categoryService.updateCategoryPosition(languageUuid, categoryUuid, form))
                    .isInstanceOf(ValidationException.class)
                    .hasFieldOrPropertyWithValue("error", ErrorCodes.CATEGORY_CIRCULAR_REFERENCE_ERROR);

            verify(categoryRepository, never()).save(any());
        }

        @Test
        void whenCircularReference_inHierarchy_throwsValidationException() {
            // Given
            final UUID parentUuid = UUID.randomUUID();
            final Category parent = new Category();
            parent.setUuid(parentUuid);

            doNothing().when(languageFacade).verifyLanguageOwnership(languageUuid);
            when(categoryRepository.findByUuidAndLanguageUuid(categoryUuid, languageUuid))
                    .thenReturn(Optional.of(existingCategory));
            when(categoryRepository.findByUuidAndLanguageUuid(parentUuid, languageUuid))
                    .thenReturn(Optional.of(parent));
            when(categoryRepository.isInParentHierarchy(parentUuid.toString(), categoryUuid.toString()))
                    .thenReturn(true);

            final UpdateCategoryPositionForm form = new UpdateCategoryPositionForm(parentUuid, 0);

            // When & Then
            ValidationException exception = assertThrows(ValidationException.class,
                    () -> categoryService.updateCategoryPosition(languageUuid, categoryUuid, form));

            assertThat(exception.getError()).isEqualTo(ErrorCodes.CATEGORY_CIRCULAR_REFERENCE_ERROR);

            verify(categoryRepository, never()).save(any());
        }

        @Test
        void whenLanguageOwnershipVerificationFails_throwsException() {
            // Given
            doThrow(new NotFoundException(ErrorCodes.LANGUAGE_NOT_FOUND, languageUuid))
                    .when(languageFacade).verifyLanguageOwnership(languageUuid);

            final UpdateCategoryPositionForm form = new UpdateCategoryPositionForm(null, 0);

            // When & Then
            assertThrows(NotFoundException.class,
                    () -> categoryService.updateCategoryPosition(languageUuid, categoryUuid, form));

            verify(categoryRepository, never()).save(any());
        }
    }
}

