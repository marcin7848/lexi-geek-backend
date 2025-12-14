package io.learn.lexigeek.word.domain;

import io.learn.lexigeek.category.CategoryFacade;
import io.learn.lexigeek.category.domain.CategoryMode;
import io.learn.lexigeek.category.dto.CategoryDto;
import io.learn.lexigeek.common.exception.NotFoundException;
import io.learn.lexigeek.common.pageable.OrderString;
import io.learn.lexigeek.common.pageable.PageDto;
import io.learn.lexigeek.common.pageable.PageableRequest;
import io.learn.lexigeek.common.validation.ErrorCodes;
import io.learn.lexigeek.language.LanguageFacade;
import io.learn.lexigeek.task.TaskFacade;
import io.learn.lexigeek.word.dto.UpdateWordCategoriesForm;
import io.learn.lexigeek.word.dto.WordDto;
import io.learn.lexigeek.word.dto.WordFilterForm;
import io.learn.lexigeek.word.dto.WordForm;
import io.learn.lexigeek.word.dto.WordPartForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WordServiceTest {

    private final WordRepository wordRepository = mock(WordRepository.class);
    private final WordStatsRepository wordStatsRepository = mock(WordStatsRepository.class);
    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final CategoryFacade categoryFacade = mock(CategoryFacade.class);
    private final LanguageFacade languageFacade = mock(LanguageFacade.class);
    private final TaskFacade taskFacade = mock(TaskFacade.class);
    private final WordService wordService = new WordService(wordRepository, wordStatsRepository, categoryRepository,
            categoryFacade, languageFacade, taskFacade);

    private UUID languageUuid;
    private UUID categoryUuid;
    private Category category;

    @BeforeEach
    void setUp() {
        languageUuid = UUID.randomUUID();
        categoryUuid = UUID.randomUUID();
        category = mock(Category.class);
        when(category.getUuid()).thenReturn(categoryUuid);
        when(category.getName()).thenReturn("Vocabulary");
        when(category.getMode()).thenReturn(CategoryMode.EXERCISE);
    }

    @Nested
    class GetWordsTests {

        @Test
        void success_returnsWords() {
            // Given
            doNothing().when(categoryFacade).verifyCategoryAccess(languageUuid, categoryUuid);

            final Word word = new Word();
            word.setUuid(UUID.randomUUID());
            word.setMechanism(WordMechanism.BASIC);
            word.setAccepted(true);
            word.setChosen(false);
            word.setComment("Test comment");

            when(wordRepository.findAll(any(WordSpecification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.singletonList(word), PageRequest.of(0, 10), 1));

            final WordFilterForm filter = new WordFilterForm(null, null, null, null, null);
            final PageableRequest pageable = new PageableRequest(1, 10, null, OrderString.asc, false);

            // When
            final PageDto<WordDto> result = wordService.getWords(languageUuid, categoryUuid, filter, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getItems()).hasSize(1);
            final WordDto dto = result.getItems().getFirst();
            assertThat(dto.mechanism()).isEqualTo(WordMechanism.BASIC);
            assertThat(dto.accepted()).isTrue();
            assertThat(dto.chosen()).isFalse();
            assertThat(dto.comment()).isEqualTo("Test comment");
            assertThat(result.getPage()).isEqualTo(1);
            assertThat(result.getPageSize()).isEqualTo(10);

            verify(categoryFacade).verifyCategoryAccess(languageUuid, categoryUuid);
        }

        @Test
        void emptyResult_returnsEmptyPage() {
            // Given
            doNothing().when(categoryFacade).verifyCategoryAccess(languageUuid, categoryUuid);

            when(wordRepository.findAll(any(WordSpecification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0));

            final WordFilterForm filter = new WordFilterForm(null, null, null, null, null);
            final PageableRequest pageable = new PageableRequest(1, 10, null, OrderString.asc, false);

            // When
            final PageDto<WordDto> result = wordService.getWords(languageUuid, categoryUuid, filter, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getItems()).isEmpty();
            assertThat(result.getPage()).isEqualTo(1);
            assertThat(result.getPageSize()).isEqualTo(10);
        }

        @Test
        void whenCategoryAccessVerificationFails_throwsException() {
            // Given
            doThrow(new NotFoundException(ErrorCodes.CATEGORY_NOT_FOUND, categoryUuid))
                    .when(categoryFacade).verifyCategoryAccess(languageUuid, categoryUuid);

            final WordFilterForm filter = new WordFilterForm(null, null, null, null, null);
            final PageableRequest pageable = new PageableRequest(1, 10, null, OrderString.asc, false);

            // When & Then
            assertThrows(NotFoundException.class,
                    () -> wordService.getWords(languageUuid, categoryUuid, filter, pageable));

            verify(wordRepository, never()).findAll(any(WordSpecification.class), any(Pageable.class));
        }
    }

    @Nested
    class GetWordTests {

        private UUID wordUuid;

        @BeforeEach
        void setUp() {
            wordUuid = UUID.randomUUID();
        }

        @Test
        void success_returnsWord() {
            // Given
            doNothing().when(categoryFacade).verifyCategoryAccess(languageUuid, categoryUuid);

            final Word word = new Word();
            word.setUuid(wordUuid);
            word.setMechanism(WordMechanism.BASIC);
            word.setAccepted(false);

            when(wordRepository.findByUuidAndCategoryUuid(wordUuid, categoryUuid))
                    .thenReturn(Optional.of(word));

            // When
            final WordDto result = wordService.getWord(languageUuid, categoryUuid, wordUuid);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.uuid()).isEqualTo(wordUuid);
            assertThat(result.mechanism()).isEqualTo(WordMechanism.BASIC);
            assertThat(result.accepted()).isFalse();

            verify(categoryFacade).verifyCategoryAccess(languageUuid, categoryUuid);
        }

        @Test
        void whenWordNotFound_throwsNotFoundException() {
            // Given
            doNothing().when(categoryFacade).verifyCategoryAccess(languageUuid, categoryUuid);

            when(wordRepository.findByUuidAndCategoryUuid(wordUuid, categoryUuid))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> wordService.getWord(languageUuid, categoryUuid, wordUuid))
                    .isInstanceOf(NotFoundException.class)
                    .hasFieldOrPropertyWithValue("error", ErrorCodes.WORD_NOT_FOUND);

            verify(categoryFacade).verifyCategoryAccess(languageUuid, categoryUuid);
        }

        @Test
        void whenCategoryAccessVerificationFails_throwsException() {
            // Given
            doThrow(new NotFoundException(ErrorCodes.CATEGORY_NOT_FOUND, categoryUuid))
                    .when(categoryFacade).verifyCategoryAccess(languageUuid, categoryUuid);

            // When & Then
            assertThrows(NotFoundException.class,
                    () -> wordService.getWord(languageUuid, categoryUuid, wordUuid));

            verify(wordRepository, never()).findByUuidAndCategoryUuid(any(), any());
        }
    }

    @Nested
    class CreateWordTests {

        private WordForm wordForm;

        @BeforeEach
        void setUp() {
            final List<WordPartForm> wordParts = List.of(
                    new WordPartForm(false, null, 0, false, false, null, "hello"),
                    new WordPartForm(true, null, 1, false, false, null, "hola")
            );
            wordForm = new WordForm("Test word", WordMechanism.BASIC, wordParts);
        }

        @Test
        void success_exerciseCategory_savesWord() {
            // Given
            doNothing().when(categoryFacade).verifyCategoryAccess(languageUuid, categoryUuid);
            when(categoryRepository.findByUuid(categoryUuid)).thenReturn(Optional.of(category));

            final Word savedWord = new Word();
            savedWord.setUuid(UUID.randomUUID());
            savedWord.setMechanism(WordMechanism.BASIC);
            savedWord.setComment("Test word");
            when(wordRepository.save(any(Word.class))).thenReturn(savedWord);

            // When
            final WordDto result = wordService.createWord(languageUuid, categoryUuid, wordForm);

            // Then
            assertThat(result).isNotNull();
            verify(categoryRepository).findByUuid(categoryUuid);

            final ArgumentCaptor<Word> captor = ArgumentCaptor.forClass(Word.class);
            verify(wordRepository).save(captor.capture());
            final Word saved = captor.getValue();
            assertThat(saved.getMechanism()).isEqualTo(WordMechanism.BASIC);
            assertThat(saved.getComment()).isEqualTo("Test word");
            assertThat(saved.getCategories()).contains(category);
            assertThat(saved.getWordParts()).hasSize(2);
        }

        @Test
        void success_dictionaryCategory_noMatchingWord_savesNewWord() {
            // Given
            when(category.getMode()).thenReturn(CategoryMode.DICTIONARY);
            final UUID category1Uuid = UUID.randomUUID();
            final UUID category2Uuid = UUID.randomUUID();

            doNothing().when(categoryFacade).verifyCategoryAccess(languageUuid, categoryUuid);
            when(categoryRepository.findByUuid(categoryUuid)).thenReturn(Optional.of(category));

            final PageDto<CategoryDto> categoriesPage = new PageDto<>();
            categoriesPage.setItems(List.of(
                    new CategoryDto(category1Uuid, null, "Cat1", null, null, 0),
                    new CategoryDto(category2Uuid, null, "Cat2", null, null, 1)
            ));
            categoriesPage.setPage(1);
            categoriesPage.setPageSize(10);
            categoriesPage.setTotal(2);
            when(categoryFacade.getCategories(any(), any(), any())).thenReturn(categoriesPage);
            when(wordRepository.findByCategoryUuids(anySet())).thenReturn(Collections.emptyList());

            final Word savedWord = new Word();
            savedWord.setUuid(UUID.randomUUID());
            when(wordRepository.save(any(Word.class))).thenReturn(savedWord);

            // When
            final WordDto result = wordService.createWord(languageUuid, categoryUuid, wordForm);

            // Then
            assertThat(result).isNotNull();
            verify(wordRepository).findByCategoryUuids(anySet());

            final ArgumentCaptor<Word> captor = ArgumentCaptor.forClass(Word.class);
            verify(wordRepository).save(captor.capture());
            final Word saved = captor.getValue();
            assertThat(saved.getCategories()).contains(category);
        }

        @Test
        void success_dictionaryCategory_matchingWordExists_mergesWords() {
            // Given
            when(category.getMode()).thenReturn(CategoryMode.DICTIONARY);
            final UUID category1Uuid = UUID.randomUUID();

            final Word existingWord = new Word();
            existingWord.setUuid(UUID.randomUUID());
            existingWord.setMechanism(WordMechanism.BASIC);
            existingWord.setAccepted(true);

            final WordPart existingPart = new WordPart();
            existingPart.setWord("hello");
            existingPart.setAnswer(false);
            existingPart.setPosition(0);
            existingWord.addWordPart(existingPart);

            doNothing().when(categoryFacade).verifyCategoryAccess(languageUuid, categoryUuid);
            when(categoryRepository.findByUuid(categoryUuid)).thenReturn(Optional.of(category));

            final PageDto<CategoryDto> categoriesPage = new PageDto<>();
            categoriesPage.setItems(List.of(new CategoryDto(category1Uuid, null, "Cat1", null, null, 0)));
            categoriesPage.setPage(1);
            categoriesPage.setPageSize(10);
            categoriesPage.setTotal(1);
            when(categoryFacade.getCategories(any(), any(), any())).thenReturn(categoriesPage);
            when(wordRepository.findByCategoryUuids(anySet())).thenReturn(List.of(existingWord));
            when(wordRepository.save(any(Word.class))).thenReturn(existingWord);

            // When
            final WordDto result = wordService.createWord(languageUuid, categoryUuid, wordForm);

            // Then
            assertThat(result).isNotNull();
            verify(wordRepository).findByCategoryUuids(anySet());

            final ArgumentCaptor<Word> captor = ArgumentCaptor.forClass(Word.class);
            verify(wordRepository).save(captor.capture());
            final Word saved = captor.getValue();
            assertThat(saved.getAccepted()).isFalse(); // Should be reset to false
            assertThat(saved.getCategories()).contains(category);
        }

        @Test
        void whenCategoryNotFound_throwsNotFoundException_andDoesNotSave() {
            // Given
            doNothing().when(categoryFacade).verifyCategoryAccess(languageUuid, categoryUuid);
            when(categoryRepository.findByUuid(categoryUuid)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> wordService.createWord(languageUuid, categoryUuid, wordForm))
                    .isInstanceOf(NotFoundException.class)
                    .hasFieldOrPropertyWithValue("error", ErrorCodes.CATEGORY_NOT_FOUND);

            verify(wordRepository, never()).save(any());
        }

        @Test
        void whenCategoryAccessVerificationFails_throwsException_andDoesNotSave() {
            // Given
            doThrow(new NotFoundException(ErrorCodes.CATEGORY_NOT_FOUND, categoryUuid))
                    .when(categoryFacade).verifyCategoryAccess(languageUuid, categoryUuid);

            // When & Then
            assertThrows(NotFoundException.class,
                    () -> wordService.createWord(languageUuid, categoryUuid, wordForm));

            verify(wordRepository, never()).save(any());
        }
    }

    @Nested
    class UpdateWordTests {

        private UUID wordUuid;
        private Word existingWord;
        private WordForm wordForm;

        @BeforeEach
        void setUp() {
            wordUuid = UUID.randomUUID();
            existingWord = new Word();
            existingWord.setUuid(wordUuid);
            existingWord.setMechanism(WordMechanism.BASIC);
            existingWord.setComment("Old comment");

            final List<WordPartForm> wordParts = List.of(
                    new WordPartForm(false, null, 0, false, false, null, "updated"),
                    new WordPartForm(true, null, 1, false, false, null, "actualizado")
            );
            wordForm = new WordForm("New comment", WordMechanism.TABLE, wordParts);
        }

        @Test
        void success_updatesAndSaves() {
            // Given
            doNothing().when(categoryFacade).verifyCategoryAccess(languageUuid, categoryUuid);
            when(wordRepository.findByUuidAndCategoryUuid(wordUuid, categoryUuid))
                    .thenReturn(Optional.of(existingWord));
            when(wordRepository.save(any(Word.class))).thenReturn(existingWord);

            // When
            final WordDto result = wordService.updateWord(languageUuid, categoryUuid, wordUuid, wordForm);

            // Then
            assertThat(result).isNotNull();

            final ArgumentCaptor<Word> captor = ArgumentCaptor.forClass(Word.class);
            verify(wordRepository).save(captor.capture());
            final Word saved = captor.getValue();
            assertThat(saved.getMechanism()).isEqualTo(WordMechanism.TABLE);
            assertThat(saved.getComment()).isEqualTo("New comment");
        }

        @Test
        void whenWordNotFound_throwsNotFoundException_andDoesNotSave() {
            // Given
            doNothing().when(categoryFacade).verifyCategoryAccess(languageUuid, categoryUuid);
            when(wordRepository.findByUuidAndCategoryUuid(wordUuid, categoryUuid))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> wordService.updateWord(languageUuid, categoryUuid, wordUuid, wordForm))
                    .isInstanceOf(NotFoundException.class)
                    .hasFieldOrPropertyWithValue("error", ErrorCodes.WORD_NOT_FOUND);

            verify(wordRepository, never()).save(any());
        }

        @Test
        void whenCategoryAccessVerificationFails_throwsException_andDoesNotSave() {
            // Given
            doThrow(new NotFoundException(ErrorCodes.CATEGORY_NOT_FOUND, categoryUuid))
                    .when(categoryFacade).verifyCategoryAccess(languageUuid, categoryUuid);

            // When & Then
            assertThrows(NotFoundException.class,
                    () -> wordService.updateWord(languageUuid, categoryUuid, wordUuid, wordForm));

            verify(wordRepository, never()).save(any());
        }
    }

    @Nested
    class DeleteWordTests {

        private UUID wordUuid;
        private Word existingWord;

        @BeforeEach
        void setUp() {
            wordUuid = UUID.randomUUID();
            existingWord = new Word();
            existingWord.setUuid(wordUuid);
        }

        @Test
        void success_deletesWord() {
            // Given
            doNothing().when(categoryFacade).verifyCategoryAccess(languageUuid, categoryUuid);
            when(wordRepository.findByUuidAndCategoryUuid(wordUuid, categoryUuid))
                    .thenReturn(Optional.of(existingWord));

            // When
            wordService.deleteWord(languageUuid, categoryUuid, wordUuid);

            // Then
            verify(wordRepository).delete(existingWord);
        }

        @Test
        void whenWordNotFound_throwsNotFoundException_andDoesNotDelete() {
            // Given
            doNothing().when(categoryFacade).verifyCategoryAccess(languageUuid, categoryUuid);
            when(wordRepository.findByUuidAndCategoryUuid(wordUuid, categoryUuid))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> wordService.deleteWord(languageUuid, categoryUuid, wordUuid))
                    .isInstanceOf(NotFoundException.class)
                    .hasFieldOrPropertyWithValue("error", ErrorCodes.WORD_NOT_FOUND);

            verify(wordRepository, never()).delete(any(Word.class));
        }

        @Test
        void whenCategoryAccessVerificationFails_throwsException_andDoesNotDelete() {
            // Given
            doThrow(new NotFoundException(ErrorCodes.CATEGORY_NOT_FOUND, categoryUuid))
                    .when(categoryFacade).verifyCategoryAccess(languageUuid, categoryUuid);

            // When & Then
            assertThrows(NotFoundException.class,
                    () -> wordService.deleteWord(languageUuid, categoryUuid, wordUuid));

            verify(wordRepository, never()).delete(any(Word.class));
        }
    }

    @Nested
    class AcceptWordTests {

        private UUID wordUuid;
        private Word existingWord;

        @BeforeEach
        void setUp() {
            wordUuid = UUID.randomUUID();
            existingWord = new Word();
            existingWord.setUuid(wordUuid);
            existingWord.setAccepted(false);
        }

        @Test
        void success_setsAcceptedToTrue() {
            // Given
            doNothing().when(categoryFacade).verifyCategoryAccess(languageUuid, categoryUuid);
            when(wordRepository.findByUuidAndCategoryUuid(wordUuid, categoryUuid))
                    .thenReturn(Optional.of(existingWord));
            when(wordRepository.save(any(Word.class))).thenReturn(existingWord);
            when(categoryRepository.findByUuid(categoryUuid)).thenReturn(Optional.of(category));
            doNothing().when(taskFacade).fillTask(any(), any(), any());

            // When
            final WordDto result = wordService.acceptWord(languageUuid, categoryUuid, wordUuid);

            // Then
            assertThat(result).isNotNull();

            final ArgumentCaptor<Word> captor = ArgumentCaptor.forClass(Word.class);
            verify(wordRepository).save(captor.capture());
            final Word saved = captor.getValue();
            assertThat(saved.getAccepted()).isTrue();
            verify(taskFacade).fillTask(any(), any(), any());
        }

        @Test
        void whenWordNotFound_throwsNotFoundException_andDoesNotSave() {
            // Given
            doNothing().when(categoryFacade).verifyCategoryAccess(languageUuid, categoryUuid);
            when(wordRepository.findByUuidAndCategoryUuid(wordUuid, categoryUuid))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> wordService.acceptWord(languageUuid, categoryUuid, wordUuid))
                    .isInstanceOf(NotFoundException.class)
                    .hasFieldOrPropertyWithValue("error", ErrorCodes.WORD_NOT_FOUND);

            verify(wordRepository, never()).save(any());
        }

        @Test
        void whenCategoryAccessVerificationFails_throwsException_andDoesNotSave() {
            // Given
            doThrow(new NotFoundException(ErrorCodes.CATEGORY_NOT_FOUND, categoryUuid))
                    .when(categoryFacade).verifyCategoryAccess(languageUuid, categoryUuid);

            // When & Then
            assertThrows(NotFoundException.class,
                    () -> wordService.acceptWord(languageUuid, categoryUuid, wordUuid));

            verify(wordRepository, never()).save(any());
        }
    }

    @Nested
    class ChooseWordTests {

        private UUID wordUuid;
        private Word existingWord;

        @BeforeEach
        void setUp() {
            wordUuid = UUID.randomUUID();
            existingWord = new Word();
            existingWord.setUuid(wordUuid);
            existingWord.setChosen(false);
        }

        @Test
        void success_togglesChosenToTrue() {
            // Given
            doNothing().when(categoryFacade).verifyCategoryAccess(languageUuid, categoryUuid);
            when(wordRepository.findByUuidAndCategoryUuid(wordUuid, categoryUuid))
                    .thenReturn(Optional.of(existingWord));
            when(wordRepository.save(any(Word.class))).thenReturn(existingWord);

            // When
            final WordDto result = wordService.chooseWord(languageUuid, categoryUuid, wordUuid);

            // Then
            assertThat(result).isNotNull();

            final ArgumentCaptor<Word> captor = ArgumentCaptor.forClass(Word.class);
            verify(wordRepository).save(captor.capture());
            final Word saved = captor.getValue();
            assertThat(saved.getChosen()).isTrue();
        }

        @Test
        void success_togglesChosenToFalse_whenAlreadyChosen() {
            // Given
            existingWord.setChosen(true);

            doNothing().when(categoryFacade).verifyCategoryAccess(languageUuid, categoryUuid);
            when(wordRepository.findByUuidAndCategoryUuid(wordUuid, categoryUuid))
                    .thenReturn(Optional.of(existingWord));
            when(wordRepository.save(any(Word.class))).thenReturn(existingWord);

            // When
            final WordDto result = wordService.chooseWord(languageUuid, categoryUuid, wordUuid);

            // Then
            assertThat(result).isNotNull();

            final ArgumentCaptor<Word> captor = ArgumentCaptor.forClass(Word.class);
            verify(wordRepository).save(captor.capture());
            final Word saved = captor.getValue();
            assertThat(saved.getChosen()).isFalse();
        }

        @Test
        void whenWordNotFound_throwsNotFoundException_andDoesNotSave() {
            // Given
            doNothing().when(categoryFacade).verifyCategoryAccess(languageUuid, categoryUuid);
            when(wordRepository.findByUuidAndCategoryUuid(wordUuid, categoryUuid))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> wordService.chooseWord(languageUuid, categoryUuid, wordUuid))
                    .isInstanceOf(NotFoundException.class)
                    .hasFieldOrPropertyWithValue("error", ErrorCodes.WORD_NOT_FOUND);

            verify(wordRepository, never()).save(any());
        }

        @Test
        void whenCategoryAccessVerificationFails_throwsException_andDoesNotSave() {
            // Given
            doThrow(new NotFoundException(ErrorCodes.CATEGORY_NOT_FOUND, categoryUuid))
                    .when(categoryFacade).verifyCategoryAccess(languageUuid, categoryUuid);

            // When & Then
            assertThrows(NotFoundException.class,
                    () -> wordService.chooseWord(languageUuid, categoryUuid, wordUuid));

            verify(wordRepository, never()).save(any());
        }
    }

    @Nested
    class UpdateWordCategoriesTests {

        private UUID wordUuid;
        private Word existingWord;
        private Category category1;
        private Category category2;
        private Category category3;

        @BeforeEach
        void setUp() {
            wordUuid = UUID.randomUUID();
            existingWord = new Word();
            existingWord.setUuid(wordUuid);
            existingWord.setMechanism(WordMechanism.BASIC);

            category1 = mock(Category.class);
            when(category1.getUuid()).thenReturn(UUID.randomUUID());
            when(category1.getName()).thenReturn("Category1");

            category2 = mock(Category.class);
            when(category2.getUuid()).thenReturn(UUID.randomUUID());
            when(category2.getName()).thenReturn("Category2");

            category3 = mock(Category.class);
            when(category3.getUuid()).thenReturn(UUID.randomUUID());
            when(category3.getName()).thenReturn("Category3");

            existingWord.addCategory(category1);
        }

        @Test
        void success_updatesCategories() {
            // Given
            final List<UUID> newCategoryUuids = List.of(category2.getUuid(), category3.getUuid());
            final UpdateWordCategoriesForm form = new UpdateWordCategoriesForm(newCategoryUuids);

            doNothing().when(categoryFacade).verifyCategoriesAccess(languageUuid, newCategoryUuids);
            when(wordRepository.findByUuidAndLanguageUuid(wordUuid, languageUuid))
                    .thenReturn(Optional.of(existingWord));
            when(categoryRepository.findAllByUuidIn(newCategoryUuids))
                    .thenReturn(List.of(category2, category3));
            when(wordRepository.save(any(Word.class))).thenReturn(existingWord);

            // When
            final WordDto result = wordService.updateWordCategories(languageUuid, wordUuid, form);

            // Then
            assertThat(result).isNotNull();

            final ArgumentCaptor<Word> captor = ArgumentCaptor.forClass(Word.class);
            verify(wordRepository).save(captor.capture());
            final Word saved = captor.getValue();
            assertThat(saved.getCategories()).containsExactlyInAnyOrder(category2, category3);
            assertThat(saved.getCategories()).doesNotContain(category1);
        }

        @Test
        void success_addsNewCategories_keepsExisting() {
            // Given
            final List<UUID> newCategoryUuids = List.of(category1.getUuid(), category2.getUuid());
            final UpdateWordCategoriesForm form = new UpdateWordCategoriesForm(newCategoryUuids);

            doNothing().when(categoryFacade).verifyCategoriesAccess(languageUuid, newCategoryUuids);
            when(wordRepository.findByUuidAndLanguageUuid(wordUuid, languageUuid))
                    .thenReturn(Optional.of(existingWord));
            when(categoryRepository.findAllByUuidIn(newCategoryUuids))
                    .thenReturn(List.of(category1, category2));
            when(wordRepository.save(any(Word.class))).thenReturn(existingWord);

            // When
            final WordDto result = wordService.updateWordCategories(languageUuid, wordUuid, form);

            // Then
            assertThat(result).isNotNull();

            final ArgumentCaptor<Word> captor = ArgumentCaptor.forClass(Word.class);
            verify(wordRepository).save(captor.capture());
            final Word saved = captor.getValue();
            assertThat(saved.getCategories()).containsExactlyInAnyOrder(category1, category2);
        }

        @Test
        void whenWordNotFound_throwsNotFoundException_andDoesNotSave() {
            // Given
            final List<UUID> newCategoryUuids = List.of(category2.getUuid());
            final UpdateWordCategoriesForm form = new UpdateWordCategoriesForm(newCategoryUuids);

            doNothing().when(categoryFacade).verifyCategoriesAccess(languageUuid, newCategoryUuids);
            when(wordRepository.findByUuidAndLanguageUuid(wordUuid, languageUuid))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> wordService.updateWordCategories(languageUuid, wordUuid, form))
                    .isInstanceOf(NotFoundException.class)
                    .hasFieldOrPropertyWithValue("error", ErrorCodes.WORD_NOT_FOUND);

            verify(wordRepository, never()).save(any());
        }

        @Test
        void whenCategoryNotFound_throwsNotFoundException_andDoesNotSave() {
            // Given
            final List<UUID> newCategoryUuids = List.of(category2.getUuid(), category3.getUuid());
            final UpdateWordCategoriesForm form = new UpdateWordCategoriesForm(newCategoryUuids);

            doNothing().when(categoryFacade).verifyCategoriesAccess(languageUuid, newCategoryUuids);
            when(wordRepository.findByUuidAndLanguageUuid(wordUuid, languageUuid))
                    .thenReturn(Optional.of(existingWord));
            when(categoryRepository.findAllByUuidIn(newCategoryUuids))
                    .thenReturn(List.of(category2)); // Only one found

            // When & Then
            assertThatThrownBy(() -> wordService.updateWordCategories(languageUuid, wordUuid, form))
                    .isInstanceOf(NotFoundException.class)
                    .hasFieldOrPropertyWithValue("error", ErrorCodes.CATEGORY_NOT_FOUND);

            verify(wordRepository, never()).save(any());
        }

        @Test
        void whenCategoryAccessVerificationFails_throwsException_andDoesNotSave() {
            // Given
            final List<UUID> newCategoryUuids = List.of(category2.getUuid());
            final UpdateWordCategoriesForm form = new UpdateWordCategoriesForm(newCategoryUuids);

            doThrow(new NotFoundException(ErrorCodes.CATEGORY_NOT_FOUND))
                    .when(categoryFacade).verifyCategoriesAccess(languageUuid, newCategoryUuids);

            // When & Then
            assertThrows(NotFoundException.class,
                    () -> wordService.updateWordCategories(languageUuid, wordUuid, form));

            verify(wordRepository, never()).save(any());
        }
    }
}
