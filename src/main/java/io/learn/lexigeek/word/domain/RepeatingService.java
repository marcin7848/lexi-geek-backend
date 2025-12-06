package io.learn.lexigeek.word.domain;

import io.learn.lexigeek.category.domain.CategoryMethod;
import io.learn.lexigeek.category.domain.CategoryMode;
import io.learn.lexigeek.common.exception.AlreadyExistsException;
import io.learn.lexigeek.common.exception.NotFoundException;
import io.learn.lexigeek.common.validation.ErrorCodes;
import io.learn.lexigeek.language.LanguageFacade;
import io.learn.lexigeek.word.RepeatingFacade;
import io.learn.lexigeek.word.dto.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class RepeatingService implements RepeatingFacade {

    private final RepeatSessionRepository repeatSessionRepository;
    private final LanguageRepository languageRepository;
    private final CategoryRepository categoryRepository;
    private final WordRepository wordRepository;
    private final LanguageFacade languageFacade;

    @Override
    @Transactional
    public RepeatSessionDto startSession(final UUID languageUuid, final StartRepeatSessionForm form) {
        languageFacade.verifyLanguageOwnership(languageUuid);

        if (repeatSessionRepository.existsByLanguageUuid(languageUuid)) {
            throw new AlreadyExistsException(ErrorCodes.REPEAT_SESSION_ALREADY_EXISTS, languageUuid);
        }

        final Language language = languageRepository.findByUuid(languageUuid)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.LANGUAGE_NOT_FOUND, languageUuid));

        final List<Category> categories = categoryRepository.findAllByUuidIn(form.categoryUuids());
        if (categories.isEmpty()) {
            throw new NotFoundException(ErrorCodes.CATEGORY_NOT_FOUND);
        }

        final List<Category> eligibleCategories = filterCategoriesByMethod(categories, form.method());
        if (eligibleCategories.isEmpty()) {
            throw new NotFoundException(ErrorCodes.CATEGORY_NOT_FOUND);
        }

        final Set<UUID> eligibleCategoryUuids = eligibleCategories.stream()
                .map(Category::getUuid)
                .collect(Collectors.toSet());

        List<Word> words = wordRepository.findByCategoryUuids(eligibleCategoryUuids);

        words = words.stream()
                .filter(Word::getAccepted)
                .filter(word -> !isWordDone(word, form.includeChosen()))
                .collect(Collectors.toList());

        words = prioritizeWords(words, form.includeChosen());

        words = selectWordsByCount(words, form.wordCount(), form.method());

        Collections.shuffle(words);

        final RepeatSession session = new RepeatSession();
        session.setLanguage(language);
        session.setMethod(form.method());
        session.setWordQueue(new ArrayList<>(words));

        final RepeatSession savedSession = repeatSessionRepository.save(session);

        return RepeatMapper.sessionToDto(savedSession);
    }

    @Override
    @Transactional(readOnly = true)
    public RepeatSessionDto getActiveSession(final UUID languageUuid) {
        languageFacade.verifyLanguageOwnership(languageUuid);

        final RepeatSession session = repeatSessionRepository.findByLanguageUuid(languageUuid)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.REPEAT_SESSION_NOT_FOUND, languageUuid));

        return RepeatMapper.sessionToDto(session);
    }

    @Override
    @Transactional(readOnly = true)
    public RepeatWordDto getNextWord(final UUID languageUuid) {
        languageFacade.verifyLanguageOwnership(languageUuid);

        final RepeatSession session = repeatSessionRepository.findByLanguageUuid(languageUuid)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.REPEAT_SESSION_NOT_FOUND, languageUuid));

        if (session.getWordQueue().isEmpty()) {
            throw new NotFoundException(ErrorCodes.NO_MORE_WORDS_IN_SESSION);
        }

        final Word word = session.getWordQueue().getFirst();
        final CategoryMethod categoryMethod = word.getCategoryMethod();

        final WordMethod wordMethod = determineWordMethod(categoryMethod, session.getMethod());

        // Get category mode (use first category's mode)
        final CategoryMode categoryMode = word.getCategories().stream()
                .findFirst()
                .map(Category::getMode)
                .orElse(CategoryMode.DICTIONARY);

        return RepeatMapper.wordToRepeatDto(word, wordMethod, categoryMode);
    }

    @Override
    @Transactional
    public CheckAnswerResultDto checkAnswer(final UUID languageUuid, final UUID wordUuid, final CheckAnswerForm form) {
        languageFacade.verifyLanguageOwnership(languageUuid);

        final RepeatSession session = repeatSessionRepository.findByLanguageUuid(languageUuid)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.REPEAT_SESSION_NOT_FOUND, languageUuid));

        // Verify word is in session queue
        if (session.getWordQueue().isEmpty() || !session.getWordQueue().get(0).getUuid().equals(wordUuid)) {
            throw new NotFoundException(ErrorCodes.WORD_NOT_IN_SESSION, wordUuid);
        }

        final Word word = session.getWordQueue().get(0);

        // Check answers
        final boolean correct = checkAnswers(word, form.answers());

        // Create word statistics entry
        final WordStats wordStats = new WordStats();
        wordStats.setCorrect(correct);
        wordStats.setMethod(convertRepeatMethodToWordMethod(session.getMethod()));
        wordStats.setAnswerTime(LocalDateTime.now());
        word.addWordStats(wordStats);

        if (correct) {
            // Update reset time based on spaced repetition
            final int repetitionCount = word.getWordStats().size();
            final LocalDateTime resetTime = calculateResetTime(repetitionCount);
            word.setResetTime(resetTime);
        } else {
            // Reset for review
            word.setResetTime(LocalDateTime.now().plusDays(1));
        }

        wordRepository.save(word);

        // Remove word from queue
        final List<Word> updatedQueue = new ArrayList<>(session.getWordQueue());
        updatedQueue.remove(0);
        session.setWordQueue(updatedQueue);

        final int wordsLeft = updatedQueue.size();
        final boolean sessionActive = !updatedQueue.isEmpty();

        if (sessionActive) {
            repeatSessionRepository.save(session);
        } else {
            // Delete session when no words left
            repeatSessionRepository.delete(session);
        }

        return new CheckAnswerResultDto(correct, wordsLeft, sessionActive);
    }

    @Override
    @Transactional
    public void resetSession(final UUID languageUuid) {
        languageFacade.verifyLanguageOwnership(languageUuid);

        final RepeatSession session = repeatSessionRepository.findByLanguageUuid(languageUuid)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.REPEAT_SESSION_NOT_FOUND, languageUuid));

        repeatSessionRepository.delete(session);
    }

    private List<Word> prioritizeWords(final List<Word> words, final Boolean includeChosen) {

    }

    private List<Category> filterCategoriesByMethod(final List<Category> categories, final CategoryMethod sessionMethod) {
        if (sessionMethod == CategoryMethod.BOTH) {
            return categories;
        }

        return categories.stream()
                .filter(category -> {
                    final CategoryMethod method = category.getMethod();
                    return method == CategoryMethod.BOTH || method == sessionMethod;
                })
                .collect(Collectors.toList());
    }

    private boolean isWordDone(final Word word, final Boolean includeChosen) {
        if (word.getWordStats().isEmpty()) {
            return false;
        }

        if(word.getChosen() && includeChosen){
            return false;
        }

        final LocalDateTime lastAnswerTime = word.getWordStats().stream()
                .map(WordStats::getAnswerTime)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        if (lastAnswerTime == null) {
            return false;
        }

        final LocalDateTime resetTime = word.getResetTime();
        return resetTime != null && lastAnswerTime.isAfter(resetTime);
    }

    private List<Word> selectWordsByCount(final List<Word> words, final int requestedCount, final CategoryMethod formMethod) {
        final List<Word> selectedWords = new ArrayList<>();
        int effectiveCount = 0;

        for (Word word : words) {
            final int wordSlots = calculateWordSlots(word, formMethod);

            if (effectiveCount + wordSlots <= requestedCount) {
                selectedWords.add(word);
                effectiveCount += wordSlots;
            }

            if (effectiveCount >= requestedCount) {
                break;
            }
        }

        return selectedWords;
    }

    private int calculateWordSlots(final Word word, final CategoryMethod formMethod) {
        if (formMethod == CategoryMethod.BOTH && word.getCategoryMethod() == CategoryMethod.BOTH) {
            return 2;
        }

        return 1;
    }

    private WordMethod determineWordMethod(final CategoryMethod categoryMethod, final CategoryMethod sessionMethod) {
        if (categoryMethod == CategoryMethod.BOTH && sessionMethod == CategoryMethod.BOTH) {
            return new Random().nextBoolean() ? WordMethod.QUESTION_TO_ANSWER : WordMethod.ANSWER_TO_QUESTION;
        }
        return categoryMethod == CategoryMethod.QUESTION_TO_ANSWER
                ? WordMethod.QUESTION_TO_ANSWER
                : WordMethod.ANSWER_TO_QUESTION;
    }

    private boolean checkAnswers(final Word word, final Map<String, String> userAnswers) {
        final Map<String, String> correctAnswers = word.getWordParts().stream()
                .filter(WordPart::getAnswer)
                .collect(Collectors.toMap(
                        wp -> String.valueOf(wp.getPosition()),
                        WordPart::getWord
                ));

        if (userAnswers.size() != correctAnswers.size()) {
            return false;
        }

        for (Map.Entry<String, String> entry : correctAnswers.entrySet()) {
            final String userAnswer = userAnswers.get(entry.getKey());
            if (userAnswer == null) {
                return false;
            }
            if (!userAnswer.trim().equalsIgnoreCase(entry.getValue().trim())) {
                return false;
            }
        }

        return true;
    }

    private LocalDateTime calculateResetTime(final int repetitionCount) {
        final int[] intervals = {1, 3, 7, 14, 30, 60, 90, 180, 365};
        final int index = Math.min(repetitionCount - 1, intervals.length - 1);
        final int days = intervals[Math.max(0, index)];
        return LocalDateTime.now().plusDays(days);
    }

    private WordMethod convertRepeatMethodToWordMethod(final CategoryMethod repeatMethod) {
        return switch (repeatMethod) {
            case QUESTION_TO_ANSWER -> WordMethod.QUESTION_TO_ANSWER;
            case ANSWER_TO_QUESTION -> WordMethod.ANSWER_TO_QUESTION;
            case BOTH -> new Random().nextBoolean() ? WordMethod.QUESTION_TO_ANSWER : WordMethod.ANSWER_TO_QUESTION;
        };
    }
}
