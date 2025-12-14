package io.learn.lexigeek.word.domain;

import io.learn.lexigeek.category.domain.CategoryMethod;
import io.learn.lexigeek.category.domain.CategoryMode;
import io.learn.lexigeek.common.exception.AlreadyExistsException;
import io.learn.lexigeek.common.exception.NotFoundException;
import io.learn.lexigeek.common.validation.ErrorCodes;
import io.learn.lexigeek.language.LanguageFacade;
import io.learn.lexigeek.task.TaskFacade;
import io.learn.lexigeek.task.dto.TaskType;
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
    private final TaskFacade taskFacade;

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

        return RepeatMapper.sessionToDto(savedSession, calculateWordsLeftInSession(savedSession));
    }

    @Override
    @Transactional(readOnly = true)
    public RepeatSessionDto getActiveSession(final UUID languageUuid) {
        languageFacade.verifyLanguageOwnership(languageUuid);

        final RepeatSession session = repeatSessionRepository.findByLanguageUuid(languageUuid)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.REPEAT_SESSION_NOT_FOUND, languageUuid));

        final int wordsLeft = calculateWordsLeftInSession(session);

        return RepeatMapper.sessionToDto(session, wordsLeft);
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

        final List<Word> wordQueue = session.getWordQueue();
        final int randomIndex = new Random().nextInt(wordQueue.size());
        final Word word = wordQueue.get(randomIndex);

        final WordMethod wordMethod = determineWordMethod(word, session.getMethod());

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

        final List<Word> wordQueue = session.getWordQueue();
        final Word word = wordQueue.stream()
                .filter(w -> w.getUuid().equals(wordUuid))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(ErrorCodes.WORD_NOT_IN_SESSION, wordUuid));

        final CheckAnswerResult answerResult = checkAnswers(word, form);
        final boolean correct = answerResult.correct();

        final WordStats wordStats = new WordStats();
        wordStats.setCorrect(correct);
        wordStats.setMethod(form.method());
        wordStats.setAnswerTime(LocalDateTime.now());
        word.addWordStats(wordStats);

        if (correct && shouldRemoveWordFromQueue(word, session.getMethod())) {
            final List<Word> updatedQueue = new ArrayList<>(session.getWordQueue());
            updatedQueue.removeIf(w -> w.getUuid().equals(wordUuid));
            session.setWordQueue(updatedQueue);

            final CategoryMode categoryMode = word.getCategories().stream().findFirst().orElseThrow().getMode();
            final TaskType taskType = categoryMode == CategoryMode.DICTIONARY
                    ? TaskType.REPEAT_DICTIONARY
                    : TaskType.REPEAT_EXERCISE;

            taskFacade.fillTask(taskType, languageUuid, 1);
        }

        wordRepository.save(word);

        final int wordsLeft = calculateWordsLeftInSession(session);
        final boolean sessionActive = wordsLeft > 0;

        if (sessionActive) {
            repeatSessionRepository.save(session);
        } else {
            repeatSessionRepository.delete(session);
        }

        return new CheckAnswerResultDto(correct, wordsLeft, sessionActive, answerResult.answerDetails());
    }

    @Override
    @Transactional
    public void resetSession(final UUID languageUuid) {
        languageFacade.verifyLanguageOwnership(languageUuid);

        final RepeatSession session = repeatSessionRepository.findByLanguageUuid(languageUuid)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.REPEAT_SESSION_NOT_FOUND, languageUuid));

        session.getWordQueue().forEach(word -> word.setResetTime(LocalDateTime.now()));

        wordRepository.saveAll(session.getWordQueue());
        repeatSessionRepository.delete(session);
    }

    private int calculateWordsLeftInSession(final RepeatSession session) {
        int totalWordsLeft = 0;

        for (final Word word : session.getWordQueue()) {
            totalWordsLeft += calculateWordSlotsRemaining(word, session.getMethod());
        }

        return totalWordsLeft;
    }

    private int calculateWordSlotsRemaining(final Word word, final CategoryMethod sessionMethod) {
        final LocalDateTime resetTime = word.getResetTime();

        final long correctStatsAfterReset = word.getWordStats().stream()
                .filter(stat -> stat.getCorrect() && stat.getAnswerTime().isAfter(resetTime))
                .count();

        if (sessionMethod == CategoryMethod.BOTH && word.getCategoryMethod() == CategoryMethod.BOTH) {
            if (correctStatsAfterReset == 0) {
                return 2;
            } else if (correctStatsAfterReset == 1) {
                return 1;
            } else {
                return 0;
            }
        }

        if (sessionMethod == CategoryMethod.BOTH && word.getCategoryMethod() != CategoryMethod.BOTH) {
            if (correctStatsAfterReset == 0) {
                return 1;
            } else {
                return 0;
            }
        }

        if (correctStatsAfterReset == 0) {
            return 1;
        } else {
            return 0;
        }
    }

    private boolean shouldRemoveWordFromQueue(final Word word, final CategoryMethod sessionMethod) {
        final LocalDateTime resetTime = word.getResetTime();
        final CategoryMethod wordMethod = word.getCategoryMethod();

        final List<WordStats> correctStatsAfterReset = word.getWordStats().stream()
                .filter(stat -> stat.getCorrect() && stat.getAnswerTime().isAfter(resetTime))
                .toList();

        if (sessionMethod == CategoryMethod.BOTH && wordMethod == CategoryMethod.BOTH) {
            final boolean hasQuestionToAnswer = correctStatsAfterReset.stream()
                    .anyMatch(stat -> stat.getMethod() == WordMethod.QUESTION_TO_ANSWER);
            final boolean hasAnswerToQuestion = correctStatsAfterReset.stream()
                    .anyMatch(stat -> stat.getMethod() == WordMethod.ANSWER_TO_QUESTION);
            return hasQuestionToAnswer && hasAnswerToQuestion;
        }

        if (sessionMethod == CategoryMethod.BOTH) {
            final WordMethod requiredMethod = wordMethod == CategoryMethod.QUESTION_TO_ANSWER
                    ? WordMethod.QUESTION_TO_ANSWER
                    : WordMethod.ANSWER_TO_QUESTION;
            return correctStatsAfterReset.stream()
                    .anyMatch(stat -> stat.getMethod() == requiredMethod);
        }

        final WordMethod requiredMethod = sessionMethod == CategoryMethod.QUESTION_TO_ANSWER
                ? WordMethod.QUESTION_TO_ANSWER
                : WordMethod.ANSWER_TO_QUESTION;
        return correctStatsAfterReset.stream()
                .anyMatch(stat -> stat.getMethod() == requiredMethod);
    }

    private List<Word> prioritizeWords(final List<Word> words, final Boolean includeChosen) {
        final List<Word> result = new ArrayList<>();
        final List<Word> chosenWords = new ArrayList<>();
        final List<Word> priorityWords = new ArrayList<>();
        final List<Word> normalWords = new ArrayList<>();

        for (final Word word : words) {
            if (includeChosen && word.getChosen()) {
                chosenWords.add(word);
            } else if (hasRecentIncorrectAttempts(word)) {
                priorityWords.add(word);
            } else {
                normalWords.add(word);
            }
        }

        if (includeChosen && !chosenWords.isEmpty()) {
            Collections.shuffle(chosenWords);
            result.addAll(chosenWords);
        }

        if (!priorityWords.isEmpty()) {
            priorityWords.sort((w1, w2) -> {
                final int count1 = countRecentIncorrectAttempts(w1);
                final int count2 = countRecentIncorrectAttempts(w2);
                return Integer.compare(count2, count1);
            });

            result.addAll(priorityWords);
        }

        if (!normalWords.isEmpty()) {
            Collections.shuffle(normalWords);
            result.addAll(normalWords);
        }

        return result;
    }

    private boolean hasRecentIncorrectAttempts(final Word word) {
        if (word.getWordStats().isEmpty()) {
            return false;
        }

        final LocalDateTime newestAnswerTime = word.getWordStats().stream()
                .map(WordStats::getAnswerTime)
                .max(LocalDateTime::compareTo)
                .orElseThrow();

        final LocalDateTime threeHoursBeforeNewest = newestAnswerTime.minusHours(3);

        return word.getWordStats().stream()
                .anyMatch(stat -> !stat.getCorrect()
                        && stat.getAnswerTime().isAfter(threeHoursBeforeNewest));
    }

    private int countRecentIncorrectAttempts(final Word word) {
        if (word.getResetTime() == null || word.getWordStats().isEmpty()) {
            return 0;
        }

        final LocalDateTime resetTime = word.getResetTime();
        final LocalDateTime threeHoursBeforeReset = resetTime.minusHours(3);

        return (int) word.getWordStats().stream()
                .filter(stat -> !stat.getCorrect()
                        && stat.getAnswerTime().isBefore(resetTime)
                        && stat.getAnswerTime().isAfter(threeHoursBeforeReset))
                .count();
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

        if (word.getChosen() && includeChosen) {
            return false;
        }

        final LocalDateTime lastAnswerTime = word.getWordStats().stream()
                .map(WordStats::getAnswerTime)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        final LocalDateTime resetTime = word.getResetTime();
        return lastAnswerTime.isAfter(resetTime);
    }

    private List<Word> selectWordsByCount(final List<Word> words, final int requestedCount, final CategoryMethod formMethod) {
        final List<Word> selectedWords = new ArrayList<>();
        int effectiveCount = 0;

        for (final Word word : words) {
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

    private WordMethod determineWordMethod(final Word word, final CategoryMethod sessionMethod) {
        final CategoryMethod categoryMethod = word.getCategoryMethod();
        final LocalDateTime resetTime = word.getResetTime();

        final List<WordStats> correctStatsAfterReset = word.getWordStats().stream()
                .filter(stat -> stat.getCorrect() && stat.getAnswerTime().isAfter(resetTime))
                .toList();

        if (sessionMethod == CategoryMethod.BOTH && categoryMethod == CategoryMethod.BOTH) {
            if (correctStatsAfterReset.isEmpty()) {
                return new Random().nextBoolean() ? WordMethod.QUESTION_TO_ANSWER : WordMethod.ANSWER_TO_QUESTION;
            } else {
                final WordMethod completedMethod = correctStatsAfterReset.getFirst().getMethod();
                return completedMethod == WordMethod.QUESTION_TO_ANSWER
                        ? WordMethod.ANSWER_TO_QUESTION
                        : WordMethod.QUESTION_TO_ANSWER;
            }
        }

        final CategoryMethod effectiveMethod = sessionMethod == CategoryMethod.BOTH ? categoryMethod : sessionMethod;
        return effectiveMethod == CategoryMethod.QUESTION_TO_ANSWER
                ? WordMethod.QUESTION_TO_ANSWER
                : WordMethod.ANSWER_TO_QUESTION;
    }

    private CheckAnswerResult checkAnswers(final Word word, final CheckAnswerForm form) {
        final boolean shouldCheckAnswerParts = form.method() == WordMethod.QUESTION_TO_ANSWER;

        final List<String> correctAnswers = word.getWordParts().stream()
                .filter(wp -> wp.getAnswer() == shouldCheckAnswerParts)
                .map(WordPart::getWord)
                .map(String::trim)
                .toList();

        if (correctAnswers.isEmpty()) {
            return new CheckAnswerResult(false, List.of());
        }

        final List<String> userAnswers = form.answers().values().stream()
                .map(String::trim)
                .toList();

        final List<String> normalizedCorrect = correctAnswers.stream()
                .map(String::toLowerCase)
                .sorted()
                .toList();

        final List<String> normalizedUser = userAnswers.stream()
                .map(String::toLowerCase)
                .sorted()
                .toList();

        final boolean allCorrect = normalizedUser.equals(normalizedCorrect);

        final List<CheckAnswerResultDto.AnswerDetail> answerDetails = new ArrayList<>();

        final List<String> unmatchedCorrect = new ArrayList<>(correctAnswers.stream()
                .map(String::toLowerCase)
                .toList());

        for (final String userAnswer : userAnswers) {
            final String normalizedUserAnswer = userAnswer.toLowerCase();
            final boolean isCorrect = unmatchedCorrect.remove(normalizedUserAnswer);

            String matchedCorrectAnswer = null;
            if (isCorrect) {
                matchedCorrectAnswer = correctAnswers.stream()
                        .filter(ca -> ca.toLowerCase().equals(normalizedUserAnswer))
                        .findFirst()
                        .orElse(normalizedUserAnswer);
            }

            answerDetails.add(new CheckAnswerResultDto.AnswerDetail(
                    userAnswer,
                    matchedCorrectAnswer,
                    isCorrect
            ));
        }

        for (final String missedAnswer : unmatchedCorrect) {
            final String originalAnswer = correctAnswers.stream()
                    .filter(ca -> ca.toLowerCase().equals(missedAnswer))
                    .findFirst()
                    .orElse(missedAnswer);

            answerDetails.add(new CheckAnswerResultDto.AnswerDetail(
                    null,
                    originalAnswer,
                    false
            ));
        }

        return new CheckAnswerResult(allCorrect, answerDetails);
    }

    private record CheckAnswerResult(boolean correct, List<CheckAnswerResultDto.AnswerDetail> answerDetails) {
    }
}
