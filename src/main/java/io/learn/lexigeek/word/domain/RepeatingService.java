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

        //here
        final Set<UUID> categoryUuidSet = new HashSet<>(form.categoryUuids());
        List<Word> words = wordRepository.findByCategoryUuids(categoryUuidSet);

        words = prioritizeWords(words);

        final int wordCount = Math.min(form.wordCount(), words.size());
        words = words.subList(0, wordCount);

        Collections.shuffle(words);
        //to here

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

    private List<Word> prioritizeWords(final List<Word> words) {
        // Sort by priority: never repeated > words needing review > oldest lastTimeRepeated
        return words.stream()
                .sorted((w1, w2) -> {
                    final int repeated1 = w1.getWordStats().size();
                    final int repeated2 = w2.getWordStats().size();

                    // Priority 1: Never repeated (words with 0 stats)
                    if (repeated1 == 0 && repeated2 > 0) return -1;
                    if (repeated1 > 0 && repeated2 == 0) return 1;

                    // If both never repeated, they're equal
                    if (repeated1 == 0 && repeated2 == 0) return 0;

                    // At this point, both words have been repeated at least once
                    final LocalDateTime lastRepeated1 = w1.getWordStats().stream()
                            .map(WordStats::getAnswerTime)
                            .max(LocalDateTime::compareTo)
                            .orElse(null);
                    final LocalDateTime lastRepeated2 = w2.getWordStats().stream()
                            .map(WordStats::getAnswerTime)
                            .max(LocalDateTime::compareTo)
                            .orElse(null);

                    // Priority 2: Words needing review (resetTime in the past or null)
                    final LocalDateTime now = LocalDateTime.now();
                    final boolean needsReview1 = w1.getResetTime() == null || w1.getResetTime().isBefore(now);
                    final boolean needsReview2 = w2.getResetTime() == null || w2.getResetTime().isBefore(now);

                    if (needsReview1 && !needsReview2) return -1;
                    if (!needsReview1 && needsReview2) return 1;

                    // Priority 3: Oldest last repeated
                    if (lastRepeated1 != null && lastRepeated2 != null) {
                        return lastRepeated1.compareTo(lastRepeated2);
                    }

                    return 0;
                })
                .collect(Collectors.toList());
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
        // Get all answer parts
        final Map<String, String> correctAnswers = word.getWordParts().stream()
                .filter(WordPart::getAnswer)
                .collect(Collectors.toMap(
                        wp -> String.valueOf(wp.getPosition()),
                        WordPart::getWord
                ));

        // Check if all positions are answered
        if (userAnswers.size() != correctAnswers.size()) {
            return false;
        }

        // Check each answer (case-insensitive)
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
        // Spaced repetition: 1 day, 3 days, 7 days, 14 days, 30 days, 60 days, etc.
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
