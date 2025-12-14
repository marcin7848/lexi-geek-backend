package io.learn.lexigeek.word.domain;

import io.learn.lexigeek.category.domain.CategoryMethod;
import io.learn.lexigeek.category.domain.CategoryMode;
import io.learn.lexigeek.common.exception.AlreadyExistsException;
import io.learn.lexigeek.common.exception.NotFoundException;
import io.learn.lexigeek.common.validation.ErrorCodes;
import io.learn.lexigeek.language.LanguageFacade;
import io.learn.lexigeek.task.TaskFacade;
import io.learn.lexigeek.word.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RepeatingServiceTest {

    private final RepeatSessionRepository repeatSessionRepository = mock(RepeatSessionRepository.class);
    private final LanguageRepository languageRepository = mock(LanguageRepository.class);
    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final WordRepository wordRepository = mock(WordRepository.class);
    private final LanguageFacade languageFacade = mock(LanguageFacade.class);
    private final TaskFacade taskFacade = mock(TaskFacade.class);
    private final RepeatingService repeatingService = new RepeatingService(
            repeatSessionRepository,
            languageRepository,
            categoryRepository,
            wordRepository,
            languageFacade,
            taskFacade
    );

    private UUID languageUuid;
    private UUID categoryUuid;
    private UUID wordUuid;
    private Language language;
    private Category category;
    private Word word;

    @BeforeEach
    void setUp() {
        languageUuid = UUID.randomUUID();
        categoryUuid = UUID.randomUUID();
        wordUuid = UUID.randomUUID();

        language = mock(Language.class);
        when(language.getUuid()).thenReturn(languageUuid);

        category = mock(Category.class);
        when(category.getUuid()).thenReturn(categoryUuid);
        when(category.getName()).thenReturn("Test Category");
        when(category.getMethod()).thenReturn(CategoryMethod.QUESTION_TO_ANSWER);
        when(category.getMode()).thenReturn(CategoryMode.EXERCISE);

        word = new Word();
        word.setUuid(wordUuid);
        word.setAccepted(true);
        word.setChosen(false);
        word.setResetTime(LocalDateTime.now().minusDays(1));
        word.addCategory(category);
    }

    @Nested
    class StartSessionTests {

        @Test
        void success_createsSession() {
            // Given
            doNothing().when(languageFacade).verifyLanguageOwnership(languageUuid);
            when(repeatSessionRepository.existsByLanguageUuid(languageUuid)).thenReturn(false);
            when(languageRepository.findByUuid(languageUuid)).thenReturn(Optional.of(language));
            when(categoryRepository.findAllByUuidIn(anyList())).thenReturn(List.of(category));

            final WordPart questionPart = new WordPart();
            questionPart.setWord("Hello");
            questionPart.setAnswer(false);
            questionPart.setPosition(0);
            word.addWordPart(questionPart);

            final WordPart answerPart = new WordPart();
            answerPart.setWord("Hola");
            answerPart.setAnswer(true);
            answerPart.setPosition(1);
            word.addWordPart(answerPart);

            when(wordRepository.findByCategoryUuids(anySet())).thenReturn(List.of(word));

            final RepeatSession savedSession = new RepeatSession();
            savedSession.setUuid(UUID.randomUUID());
            savedSession.setLanguage(language);
            savedSession.setMethod(CategoryMethod.QUESTION_TO_ANSWER);
            savedSession.setWordQueue(List.of(word));

            when(repeatSessionRepository.save(any(RepeatSession.class))).thenReturn(savedSession);

            final StartRepeatSessionForm form = new StartRepeatSessionForm(
                    List.of(categoryUuid),
                    10,
                    CategoryMethod.QUESTION_TO_ANSWER,
                    false
            );

            // When
            final RepeatSessionDto result = repeatingService.startSession(languageUuid, form);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.uuid()).isEqualTo(savedSession.getUuid());
            assertThat(result.method()).isEqualTo(CategoryMethod.QUESTION_TO_ANSWER);
            assertThat(result.wordsLeft()).isGreaterThan(0);

            verify(languageFacade).verifyLanguageOwnership(languageUuid);
            verify(repeatSessionRepository).save(any(RepeatSession.class));
        }

        @Test
        void whenSessionAlreadyExists_throwsAlreadyExistsException() {
            // Given
            doNothing().when(languageFacade).verifyLanguageOwnership(languageUuid);
            when(repeatSessionRepository.existsByLanguageUuid(languageUuid)).thenReturn(true);

            final StartRepeatSessionForm form = new StartRepeatSessionForm(
                    List.of(categoryUuid),
                    10,
                    CategoryMethod.QUESTION_TO_ANSWER,
                    false
            );

            // When & Then
            assertThatThrownBy(() -> repeatingService.startSession(languageUuid, form))
                    .isInstanceOf(AlreadyExistsException.class)
                    .hasFieldOrPropertyWithValue("error", ErrorCodes.REPEAT_SESSION_ALREADY_EXISTS);

            verify(languageRepository, never()).findByUuid(any());
        }

        @Test
        void whenLanguageNotFound_throwsNotFoundException() {
            // Given
            doNothing().when(languageFacade).verifyLanguageOwnership(languageUuid);
            when(repeatSessionRepository.existsByLanguageUuid(languageUuid)).thenReturn(false);
            when(languageRepository.findByUuid(languageUuid)).thenReturn(Optional.empty());

            final StartRepeatSessionForm form = new StartRepeatSessionForm(
                    List.of(categoryUuid),
                    10,
                    CategoryMethod.QUESTION_TO_ANSWER,
                    false
            );

            // When & Then
            assertThatThrownBy(() -> repeatingService.startSession(languageUuid, form))
                    .isInstanceOf(NotFoundException.class)
                    .hasFieldOrPropertyWithValue("error", ErrorCodes.LANGUAGE_NOT_FOUND);

            verify(categoryRepository, never()).findAllByUuidIn(anyList());
        }

        @Test
        void whenNoCategoriesFound_throwsNotFoundException() {
            // Given
            doNothing().when(languageFacade).verifyLanguageOwnership(languageUuid);
            when(repeatSessionRepository.existsByLanguageUuid(languageUuid)).thenReturn(false);
            when(languageRepository.findByUuid(languageUuid)).thenReturn(Optional.of(language));
            when(categoryRepository.findAllByUuidIn(anyList())).thenReturn(Collections.emptyList());

            final StartRepeatSessionForm form = new StartRepeatSessionForm(
                    List.of(categoryUuid),
                    10,
                    CategoryMethod.QUESTION_TO_ANSWER,
                    false
            );

            // When & Then
            assertThatThrownBy(() -> repeatingService.startSession(languageUuid, form))
                    .isInstanceOf(NotFoundException.class)
                    .hasFieldOrPropertyWithValue("error", ErrorCodes.CATEGORY_NOT_FOUND);

            verify(wordRepository, never()).findByCategoryUuids(anySet());
        }

        @Test
        void withMethodBoth_createsSessionWithMultipleMethods() {
            // Given
            when(category.getMethod()).thenReturn(CategoryMethod.BOTH);
            doNothing().when(languageFacade).verifyLanguageOwnership(languageUuid);
            when(repeatSessionRepository.existsByLanguageUuid(languageUuid)).thenReturn(false);
            when(languageRepository.findByUuid(languageUuid)).thenReturn(Optional.of(language));
            when(categoryRepository.findAllByUuidIn(anyList())).thenReturn(List.of(category));

            final WordPart questionPart = new WordPart();
            questionPart.setWord("Hello");
            questionPart.setAnswer(false);
            questionPart.setPosition(0);
            word.addWordPart(questionPart);

            final WordPart answerPart = new WordPart();
            answerPart.setWord("Hola");
            answerPart.setAnswer(true);
            answerPart.setPosition(1);
            word.addWordPart(answerPart);

            when(wordRepository.findByCategoryUuids(anySet())).thenReturn(List.of(word));

            final RepeatSession savedSession = new RepeatSession();
            savedSession.setUuid(UUID.randomUUID());
            savedSession.setLanguage(language);
            savedSession.setMethod(CategoryMethod.BOTH);
            savedSession.setWordQueue(List.of(word));

            when(repeatSessionRepository.save(any(RepeatSession.class))).thenReturn(savedSession);

            final StartRepeatSessionForm form = new StartRepeatSessionForm(
                    List.of(categoryUuid),
                    10,
                    CategoryMethod.BOTH,
                    false
            );

            // When
            final RepeatSessionDto result = repeatingService.startSession(languageUuid, form);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.method()).isEqualTo(CategoryMethod.BOTH);
            verify(repeatSessionRepository).save(any(RepeatSession.class));
        }

        @Test
        void withIncludeChosen_includesChosenWords() {
            // Given
            word.setChosen(true);
            doNothing().when(languageFacade).verifyLanguageOwnership(languageUuid);
            when(repeatSessionRepository.existsByLanguageUuid(languageUuid)).thenReturn(false);
            when(languageRepository.findByUuid(languageUuid)).thenReturn(Optional.of(language));
            when(categoryRepository.findAllByUuidIn(anyList())).thenReturn(List.of(category));

            final WordPart questionPart = new WordPart();
            questionPart.setWord("Hello");
            questionPart.setAnswer(false);
            questionPart.setPosition(0);
            word.addWordPart(questionPart);

            final WordPart answerPart = new WordPart();
            answerPart.setWord("Hola");
            answerPart.setAnswer(true);
            answerPart.setPosition(1);
            word.addWordPart(answerPart);

            when(wordRepository.findByCategoryUuids(anySet())).thenReturn(List.of(word));

            final RepeatSession savedSession = new RepeatSession();
            savedSession.setUuid(UUID.randomUUID());
            savedSession.setLanguage(language);
            savedSession.setMethod(CategoryMethod.QUESTION_TO_ANSWER);
            savedSession.setWordQueue(List.of(word));

            when(repeatSessionRepository.save(any(RepeatSession.class))).thenReturn(savedSession);

            final StartRepeatSessionForm form = new StartRepeatSessionForm(
                    List.of(categoryUuid),
                    10,
                    CategoryMethod.QUESTION_TO_ANSWER,
                    true
            );

            // When
            final RepeatSessionDto result = repeatingService.startSession(languageUuid, form);

            // Then
            assertThat(result).isNotNull();
            verify(repeatSessionRepository).save(any(RepeatSession.class));
        }

        @Test
        void whenLanguageOwnershipVerificationFails_throwsException() {
            // Given
            doThrow(new NotFoundException(ErrorCodes.LANGUAGE_NOT_FOUND, languageUuid))
                    .when(languageFacade).verifyLanguageOwnership(languageUuid);

            final StartRepeatSessionForm form = new StartRepeatSessionForm(
                    List.of(categoryUuid),
                    10,
                    CategoryMethod.QUESTION_TO_ANSWER,
                    false
            );

            // When & Then
            assertThatThrownBy(() -> repeatingService.startSession(languageUuid, form))
                    .isInstanceOf(NotFoundException.class);

            verify(repeatSessionRepository, never()).existsByLanguageUuid(any());
        }
    }

    @Nested
    class GetActiveSessionTests {

        @Test
        void success_returnsActiveSession() {
            // Given
            doNothing().when(languageFacade).verifyLanguageOwnership(languageUuid);

            final RepeatSession session = new RepeatSession();
            session.setUuid(UUID.randomUUID());
            session.setLanguage(language);
            session.setMethod(CategoryMethod.QUESTION_TO_ANSWER);
            session.setWordQueue(List.of(word));

            when(repeatSessionRepository.findByLanguageUuid(languageUuid)).thenReturn(Optional.of(session));

            // When
            final RepeatSessionDto result = repeatingService.getActiveSession(languageUuid);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.uuid()).isEqualTo(session.getUuid());
            assertThat(result.method()).isEqualTo(CategoryMethod.QUESTION_TO_ANSWER);

            verify(languageFacade).verifyLanguageOwnership(languageUuid);
            verify(repeatSessionRepository).findByLanguageUuid(languageUuid);
        }

        @Test
        void whenSessionNotFound_throwsNotFoundException() {
            // Given
            doNothing().when(languageFacade).verifyLanguageOwnership(languageUuid);
            when(repeatSessionRepository.findByLanguageUuid(languageUuid)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> repeatingService.getActiveSession(languageUuid))
                    .isInstanceOf(NotFoundException.class)
                    .hasFieldOrPropertyWithValue("error", ErrorCodes.REPEAT_SESSION_NOT_FOUND);

            verify(languageFacade).verifyLanguageOwnership(languageUuid);
        }

        @Test
        void whenLanguageOwnershipVerificationFails_throwsException() {
            // Given
            doThrow(new NotFoundException(ErrorCodes.LANGUAGE_NOT_FOUND, languageUuid))
                    .when(languageFacade).verifyLanguageOwnership(languageUuid);

            // When & Then
            assertThatThrownBy(() -> repeatingService.getActiveSession(languageUuid))
                    .isInstanceOf(NotFoundException.class);

            verify(repeatSessionRepository, never()).findByLanguageUuid(any());
        }
    }

    @Nested
    class GetNextWordTests {

        @Test
        void success_returnsNextWord() {
            // Given
            doNothing().when(languageFacade).verifyLanguageOwnership(languageUuid);

            final RepeatSession session = new RepeatSession();
            session.setUuid(UUID.randomUUID());
            session.setLanguage(language);
            session.setMethod(CategoryMethod.QUESTION_TO_ANSWER);
            session.setWordQueue(new ArrayList<>(List.of(word)));

            final WordPart questionPart = new WordPart();
            questionPart.setWord("Hello");
            questionPart.setAnswer(false);
            questionPart.setPosition(0);
            word.addWordPart(questionPart);

            final WordPart answerPart = new WordPart();
            answerPart.setWord("Hola");
            answerPart.setAnswer(true);
            answerPart.setPosition(1);
            word.addWordPart(answerPart);

            when(repeatSessionRepository.findByLanguageUuid(languageUuid)).thenReturn(Optional.of(session));

            // When
            final RepeatWordDto result = repeatingService.getNextWord(languageUuid);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.uuid()).isEqualTo(wordUuid);
            assertThat(result.method()).isIn(WordMethod.QUESTION_TO_ANSWER, WordMethod.ANSWER_TO_QUESTION);

            verify(languageFacade).verifyLanguageOwnership(languageUuid);
        }

        @Test
        void whenWordQueueEmpty_throwsNotFoundException() {
            // Given
            doNothing().when(languageFacade).verifyLanguageOwnership(languageUuid);

            final RepeatSession session = new RepeatSession();
            session.setUuid(UUID.randomUUID());
            session.setLanguage(language);
            session.setMethod(CategoryMethod.QUESTION_TO_ANSWER);
            session.setWordQueue(new ArrayList<>());

            when(repeatSessionRepository.findByLanguageUuid(languageUuid)).thenReturn(Optional.of(session));

            // When & Then
            assertThatThrownBy(() -> repeatingService.getNextWord(languageUuid))
                    .isInstanceOf(NotFoundException.class)
                    .hasFieldOrPropertyWithValue("error", ErrorCodes.NO_MORE_WORDS_IN_SESSION);

            verify(languageFacade).verifyLanguageOwnership(languageUuid);
        }

        @Test
        void whenSessionNotFound_throwsNotFoundException() {
            // Given
            doNothing().when(languageFacade).verifyLanguageOwnership(languageUuid);
            when(repeatSessionRepository.findByLanguageUuid(languageUuid)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> repeatingService.getNextWord(languageUuid))
                    .isInstanceOf(NotFoundException.class)
                    .hasFieldOrPropertyWithValue("error", ErrorCodes.REPEAT_SESSION_NOT_FOUND);

            verify(languageFacade).verifyLanguageOwnership(languageUuid);
        }

        @Test
        void whenLanguageOwnershipVerificationFails_throwsException() {
            // Given
            doThrow(new NotFoundException(ErrorCodes.LANGUAGE_NOT_FOUND, languageUuid))
                    .when(languageFacade).verifyLanguageOwnership(languageUuid);

            // When & Then
            assertThatThrownBy(() -> repeatingService.getNextWord(languageUuid))
                    .isInstanceOf(NotFoundException.class);

            verify(repeatSessionRepository, never()).findByLanguageUuid(any());
        }
    }

    @Nested
    class CheckAnswerTests {

        @Test
        void success_correctAnswer_removesWordFromQueue() {
            // Given
            doNothing().when(languageFacade).verifyLanguageOwnership(languageUuid);

            final RepeatSession session = new RepeatSession();
            session.setUuid(UUID.randomUUID());
            session.setLanguage(language);
            session.setMethod(CategoryMethod.QUESTION_TO_ANSWER);
            session.setWordQueue(new ArrayList<>(List.of(word)));

            final WordPart questionPart = new WordPart();
            questionPart.setWord("Hello");
            questionPart.setAnswer(false);
            questionPart.setPosition(0);
            word.addWordPart(questionPart);

            final WordPart answerPart = new WordPart();
            answerPart.setWord("Hola");
            answerPart.setAnswer(true);
            answerPart.setPosition(1);
            word.addWordPart(answerPart);

            when(repeatSessionRepository.findByLanguageUuid(languageUuid)).thenReturn(Optional.of(session));
            when(wordRepository.save(any(Word.class))).thenReturn(word);

            final CheckAnswerForm form = new CheckAnswerForm(
                    Map.of("0", "Hola"),
                    WordMethod.QUESTION_TO_ANSWER
            );

            // When
            final CheckAnswerResultDto result = repeatingService.checkAnswer(languageUuid, wordUuid, form);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.correct()).isTrue();
            assertThat(result.wordsLeft()).isEqualTo(0);
            assertThat(result.sessionActive()).isFalse();

            verify(wordRepository).save(any(Word.class));
            verify(repeatSessionRepository).delete(session);
        }

        @Test
        void success_incorrectAnswer_keepsWordInQueue() {
            // Given
            doNothing().when(languageFacade).verifyLanguageOwnership(languageUuid);

            final RepeatSession session = new RepeatSession();
            session.setUuid(UUID.randomUUID());
            session.setLanguage(language);
            session.setMethod(CategoryMethod.QUESTION_TO_ANSWER);
            session.setWordQueue(new ArrayList<>(List.of(word)));

            final WordPart questionPart = new WordPart();
            questionPart.setWord("Hello");
            questionPart.setAnswer(false);
            questionPart.setPosition(0);
            word.addWordPart(questionPart);

            final WordPart answerPart = new WordPart();
            answerPart.setWord("Hola");
            answerPart.setAnswer(true);
            answerPart.setPosition(1);
            word.addWordPart(answerPart);

            when(repeatSessionRepository.findByLanguageUuid(languageUuid)).thenReturn(Optional.of(session));
            when(wordRepository.save(any(Word.class))).thenReturn(word);
            when(repeatSessionRepository.save(any(RepeatSession.class))).thenReturn(session);

            final CheckAnswerForm form = new CheckAnswerForm(
                    Map.of("0", "Wrong"),
                    WordMethod.QUESTION_TO_ANSWER
            );

            // When
            final CheckAnswerResultDto result = repeatingService.checkAnswer(languageUuid, wordUuid, form);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.correct()).isFalse();
            assertThat(result.wordsLeft()).isGreaterThan(0);
            assertThat(result.sessionActive()).isTrue();

            verify(wordRepository).save(any(Word.class));
            verify(repeatSessionRepository).save(session);
            verify(repeatSessionRepository, never()).delete(any());
        }

        @Test
        void whenWordNotInSession_throwsNotFoundException() {
            // Given
            doNothing().when(languageFacade).verifyLanguageOwnership(languageUuid);

            final RepeatSession session = new RepeatSession();
            session.setUuid(UUID.randomUUID());
            session.setLanguage(language);
            session.setMethod(CategoryMethod.QUESTION_TO_ANSWER);
            session.setWordQueue(new ArrayList<>());

            when(repeatSessionRepository.findByLanguageUuid(languageUuid)).thenReturn(Optional.of(session));

            final CheckAnswerForm form = new CheckAnswerForm(
                    Map.of("0", "Hola"),
                    WordMethod.QUESTION_TO_ANSWER
            );

            // When & Then
            assertThatThrownBy(() -> repeatingService.checkAnswer(languageUuid, wordUuid, form))
                    .isInstanceOf(NotFoundException.class)
                    .hasFieldOrPropertyWithValue("error", ErrorCodes.WORD_NOT_IN_SESSION);

            verify(wordRepository, never()).save(any());
        }

        @Test
        void whenSessionNotFound_throwsNotFoundException() {
            // Given
            doNothing().when(languageFacade).verifyLanguageOwnership(languageUuid);
            when(repeatSessionRepository.findByLanguageUuid(languageUuid)).thenReturn(Optional.empty());

            final CheckAnswerForm form = new CheckAnswerForm(
                    Map.of("0", "Hola"),
                    WordMethod.QUESTION_TO_ANSWER
            );

            // When & Then
            assertThatThrownBy(() -> repeatingService.checkAnswer(languageUuid, wordUuid, form))
                    .isInstanceOf(NotFoundException.class)
                    .hasFieldOrPropertyWithValue("error", ErrorCodes.REPEAT_SESSION_NOT_FOUND);

            verify(wordRepository, never()).save(any());
        }

        @Test
        void withAnswerToQuestionMethod_checksQuestionParts() {
            // Given
            doNothing().when(languageFacade).verifyLanguageOwnership(languageUuid);

            final RepeatSession session = new RepeatSession();
            session.setUuid(UUID.randomUUID());
            session.setLanguage(language);
            session.setMethod(CategoryMethod.ANSWER_TO_QUESTION);
            session.setWordQueue(new ArrayList<>(List.of(word)));

            final WordPart questionPart = new WordPart();
            questionPart.setWord("Hello");
            questionPart.setAnswer(false);
            questionPart.setPosition(0);
            word.addWordPart(questionPart);

            final WordPart answerPart = new WordPart();
            answerPart.setWord("Hola");
            answerPart.setAnswer(true);
            answerPart.setPosition(1);
            word.addWordPart(answerPart);

            when(repeatSessionRepository.findByLanguageUuid(languageUuid)).thenReturn(Optional.of(session));
            when(wordRepository.save(any(Word.class))).thenReturn(word);

            final CheckAnswerForm form = new CheckAnswerForm(
                    Map.of("0", "Hello"),
                    WordMethod.ANSWER_TO_QUESTION
            );

            // When
            final CheckAnswerResultDto result = repeatingService.checkAnswer(languageUuid, wordUuid, form);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.correct()).isTrue();

            verify(wordRepository).save(any(Word.class));
        }

        @Test
        void withMultipleAnswers_checksAllAnswers() {
            // Given
            doNothing().when(languageFacade).verifyLanguageOwnership(languageUuid);

            final RepeatSession session = new RepeatSession();
            session.setUuid(UUID.randomUUID());
            session.setLanguage(language);
            session.setMethod(CategoryMethod.QUESTION_TO_ANSWER);
            session.setWordQueue(new ArrayList<>(List.of(word)));

            final WordPart questionPart = new WordPart();
            questionPart.setWord("Hello");
            questionPart.setAnswer(false);
            questionPart.setPosition(0);
            word.addWordPart(questionPart);

            final WordPart answerPart1 = new WordPart();
            answerPart1.setWord("Hola");
            answerPart1.setAnswer(true);
            answerPart1.setPosition(1);
            word.addWordPart(answerPart1);

            final WordPart answerPart2 = new WordPart();
            answerPart2.setWord("Buenos días");
            answerPart2.setAnswer(true);
            answerPart2.setPosition(2);
            word.addWordPart(answerPart2);

            when(repeatSessionRepository.findByLanguageUuid(languageUuid)).thenReturn(Optional.of(session));
            when(wordRepository.save(any(Word.class))).thenReturn(word);

            final CheckAnswerForm form = new CheckAnswerForm(
                    Map.of("0", "Hola", "1", "Buenos días"),
                    WordMethod.QUESTION_TO_ANSWER
            );

            // When
            final CheckAnswerResultDto result = repeatingService.checkAnswer(languageUuid, wordUuid, form);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.correct()).isTrue();
            assertThat(result.answerDetails()).hasSize(2);

            verify(wordRepository).save(any(Word.class));
        }

        @Test
        void whenLanguageOwnershipVerificationFails_throwsException() {
            // Given
            doThrow(new NotFoundException(ErrorCodes.LANGUAGE_NOT_FOUND, languageUuid))
                    .when(languageFacade).verifyLanguageOwnership(languageUuid);

            final CheckAnswerForm form = new CheckAnswerForm(
                    Map.of("0", "Hola"),
                    WordMethod.QUESTION_TO_ANSWER
            );

            // When & Then
            assertThatThrownBy(() -> repeatingService.checkAnswer(languageUuid, wordUuid, form))
                    .isInstanceOf(NotFoundException.class);

            verify(repeatSessionRepository, never()).findByLanguageUuid(any());
        }
    }

    @Nested
    class ResetSessionTests {

        @Test
        void success_resetsSessionAndDeletesIt() {
            // Given
            doNothing().when(languageFacade).verifyLanguageOwnership(languageUuid);

            final RepeatSession session = new RepeatSession();
            session.setUuid(UUID.randomUUID());
            session.setLanguage(language);
            session.setMethod(CategoryMethod.QUESTION_TO_ANSWER);
            session.setWordQueue(new ArrayList<>(List.of(word)));

            when(repeatSessionRepository.findByLanguageUuid(languageUuid)).thenReturn(Optional.of(session));
            when(wordRepository.saveAll(any())).thenReturn(List.of(word));

            // When
            repeatingService.resetSession(languageUuid);

            // Then
            verify(languageFacade).verifyLanguageOwnership(languageUuid);
            verify(wordRepository).saveAll(any());
            verify(repeatSessionRepository).delete(session);
        }

        @Test
        void success_updatesResetTimeForAllWords() {
            // Given
            doNothing().when(languageFacade).verifyLanguageOwnership(languageUuid);

            final Word word2 = new Word();
            word2.setUuid(UUID.randomUUID());
            word2.setResetTime(LocalDateTime.now().minusDays(2));

            final RepeatSession session = new RepeatSession();
            session.setUuid(UUID.randomUUID());
            session.setLanguage(language);
            session.setMethod(CategoryMethod.QUESTION_TO_ANSWER);
            session.setWordQueue(new ArrayList<>(List.of(word, word2)));

            when(repeatSessionRepository.findByLanguageUuid(languageUuid)).thenReturn(Optional.of(session));
            when(wordRepository.saveAll(any())).thenReturn(List.of(word, word2));

            final LocalDateTime beforeReset = LocalDateTime.now();

            // When
            repeatingService.resetSession(languageUuid);

            // Then
            @SuppressWarnings("unchecked") final ArgumentCaptor<List<Word>> wordsCaptor = ArgumentCaptor.forClass(List.class);
            verify(wordRepository).saveAll(wordsCaptor.capture());

            final List<Word> savedWords = wordsCaptor.getValue();
            assertThat(savedWords).hasSize(2);
            savedWords.forEach(w -> assertThat(w.getResetTime()).isAfterOrEqualTo(beforeReset));
        }

        @Test
        void whenSessionNotFound_throwsNotFoundException() {
            // Given
            doNothing().when(languageFacade).verifyLanguageOwnership(languageUuid);
            when(repeatSessionRepository.findByLanguageUuid(languageUuid)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> repeatingService.resetSession(languageUuid))
                    .isInstanceOf(NotFoundException.class)
                    .hasFieldOrPropertyWithValue("error", ErrorCodes.REPEAT_SESSION_NOT_FOUND);

            verify(wordRepository, never()).saveAll(any());
            verify(repeatSessionRepository, never()).delete(any());
        }

        @Test
        void whenLanguageOwnershipVerificationFails_throwsException() {
            // Given
            doThrow(new NotFoundException(ErrorCodes.LANGUAGE_NOT_FOUND, languageUuid))
                    .when(languageFacade).verifyLanguageOwnership(languageUuid);

            // When & Then
            assertThatThrownBy(() -> repeatingService.resetSession(languageUuid))
                    .isInstanceOf(NotFoundException.class);

            verify(repeatSessionRepository, never()).findByLanguageUuid(any());
        }
    }
}

