package io.learn.lexigeek.word.domain;

import io.learn.lexigeek.category.CategoryFacade;
import io.learn.lexigeek.category.domain.CategoryMode;
import io.learn.lexigeek.category.dto.CategoryDto;
import io.learn.lexigeek.category.dto.CategoryFilterForm;
import io.learn.lexigeek.common.exception.NotFoundException;
import io.learn.lexigeek.common.pageable.PageDto;
import io.learn.lexigeek.common.pageable.PageableRequest;
import io.learn.lexigeek.common.pageable.PageableUtils;
import io.learn.lexigeek.common.pageable.SortOrder;
import io.learn.lexigeek.common.validation.ErrorCodes;
import io.learn.lexigeek.language.LanguageFacade;
import io.learn.lexigeek.task.TaskFacade;
import io.learn.lexigeek.task.dto.TaskType;
import io.learn.lexigeek.word.WordFacade;
import io.learn.lexigeek.word.dto.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static java.util.stream.Collectors.toSet;

@Service
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class WordService implements WordFacade {

    private final WordRepository wordRepository;
    private final WordStatsRepository wordStatsRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryFacade categoryFacade;
    private final LanguageFacade languageFacade;
    private final TaskFacade taskFacade;

    @Override
    public PageDto<WordDto> getWords(final UUID languageUuid, final UUID categoryUuid,
                                     final WordFilterForm form, final PageableRequest pageableRequest) {
        pageableRequest.addDefaultSorts(new SortOrder(Word.Fields.created, Sort.Direction.DESC));

        categoryFacade.verifyCategoryAccess(languageUuid, categoryUuid);

        final WordSpecification specification = new WordSpecification(form, categoryUuid);


        final String sortField = pageableRequest.getFirstSort().getField();
        final boolean sortByComputedField = "lastTimeRepeated".equals(sortField) || "repeated".equals(sortField);

        if (sortByComputedField) {
            final List<Word> allWords = wordRepository.findAll(specification);
            final List<WordDto> wordDtos = allWords.stream()
                    .map(WordMapper::entityToDto)
                    .toList();

            final Map<String, Comparator<WordDto>> customComparators = Map.of(
                    "lastTimeRepeated", Comparator.comparing(WordDto::lastTimeRepeated,
                            Comparator.nullsLast(Comparator.naturalOrder())),
                    "repeated", Comparator.comparing(WordDto::repeated,
                            Comparator.nullsLast(Comparator.naturalOrder()))
            );

            return PageableUtils.listToPageDto(wordDtos, pageableRequest, customComparators);
        }

        return PageableUtils.toDto(wordRepository.findAll(specification, PageableUtils.createPageable(pageableRequest))
                .map(WordMapper::entityToDto), pageableRequest);
    }


    @Override
    public WordDto getWord(final UUID languageUuid, final UUID categoryUuid, final UUID wordUuid) {
        categoryFacade.verifyCategoryAccess(languageUuid, categoryUuid);

        final Word word = wordRepository.findByUuidAndCategoryUuid(wordUuid, categoryUuid)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.WORD_NOT_FOUND, wordUuid));

        return WordMapper.entityToDto(word);
    }

    @Override
    public WordDto createWord(final UUID languageUuid, final UUID categoryUuid, final WordForm form) {
        categoryFacade.verifyCategoryAccess(languageUuid, categoryUuid);

        final Category category = categoryRepository.findByUuid(categoryUuid)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.CATEGORY_NOT_FOUND, categoryUuid));

        final Word word = WordMapper.formToEntity(form);

        if (category.getMode() == CategoryMode.DICTIONARY) {
            final Set<UUID> categories = categoryFacade.getCategories(
                            languageUuid,
                            new CategoryFilterForm(null, null, null, CategoryMode.DICTIONARY, null, null),
                            PageableRequest.builder().singlePage(true).build())
                    .getItems().stream()
                    .map(CategoryDto::uuid)
                    .collect(toSet());

            final List<Word> existingWords = wordRepository.findByCategoryUuids(categories);

            final Word matchingWord = findWordWithMatchingParts(existingWords, word);

            if (matchingWord != null) {
                mergeWordParts(matchingWord, word);

                if (!matchingWord.getCategories().contains(category)) {
                    matchingWord.addCategory(category);
                }

                matchingWord.setAccepted(false);

                final Word savedWord = wordRepository.save(matchingWord);
                return WordMapper.entityToDto(savedWord);
            }
        }

        word.addCategory(category);
        final Word savedWord = wordRepository.save(word);
        return WordMapper.entityToDto(savedWord);
    }

    @Override
    public WordDto updateWord(final UUID languageUuid, final UUID categoryUuid,
                              final UUID wordUuid, final WordForm form) {
        categoryFacade.verifyCategoryAccess(languageUuid, categoryUuid);

        final Word word = wordRepository.findByUuidAndCategoryUuid(wordUuid, categoryUuid)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.WORD_NOT_FOUND, wordUuid));

        WordMapper.updateEntityFromForm(word, form);

        final Word savedWord = wordRepository.save(word);
        return WordMapper.entityToDto(savedWord);
    }

    @Override
    @Transactional
    public void deleteWord(final UUID languageUuid, final UUID categoryUuid, final UUID wordUuid) {
        categoryFacade.verifyCategoryAccess(languageUuid, categoryUuid);

        final Word word = wordRepository.findByUuidAndCategoryUuid(wordUuid, categoryUuid)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.WORD_NOT_FOUND, wordUuid));

        wordRepository.delete(word);
    }

    @Override
    @Transactional
    public WordDto acceptWord(final UUID languageUuid, final UUID categoryUuid, final UUID wordUuid) {
        categoryFacade.verifyCategoryAccess(languageUuid, categoryUuid);

        final Word word = wordRepository.findByUuidAndCategoryUuid(wordUuid, categoryUuid)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.WORD_NOT_FOUND, wordUuid));

        word.setAccepted(true);
        final Word savedWord = wordRepository.save(word);

        final Category category = categoryRepository.findByUuid(categoryUuid)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.CATEGORY_NOT_FOUND, categoryUuid));
        final TaskType taskType = category.getMode() == CategoryMode.DICTIONARY
                ? TaskType.ADD_DICTIONARY
                : TaskType.ADD_EXERCISE;
        taskFacade.fillTask(taskType, languageUuid, 1);

        return WordMapper.entityToDto(savedWord);
    }

    @Override
    public WordDto chooseWord(final UUID languageUuid, final UUID categoryUuid, final UUID wordUuid) {
        categoryFacade.verifyCategoryAccess(languageUuid, categoryUuid);

        final Word word = wordRepository.findByUuidAndCategoryUuid(wordUuid, categoryUuid)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.WORD_NOT_FOUND, wordUuid));

        word.setChosen(!word.getChosen());
        final Word savedWord = wordRepository.save(word);
        return WordMapper.entityToDto(savedWord);
    }

    private Word findWordWithMatchingParts(final List<Word> existingWords, final Word newWord) {
        return existingWords.stream()
                .filter(existingWord -> hasMatchingWordParts(existingWord, newWord))
                .findFirst()
                .orElse(null);
    }

    private boolean hasMatchingWordParts(final Word existingWord, final Word newWord) {
        return existingWord.getWordParts().stream().anyMatch(existingPart ->
                newWord.getWordParts().stream().anyMatch(
                        newPart -> areWordPartsEqual(existingPart, newPart)
                )
        );
    }

    private boolean areWordPartsEqual(final WordPart part1, final WordPart part2) {
        return Objects.equals(part1.getWord(), part2.getWord()) && part1.getAnswer().equals(part2.getAnswer());
    }

    private void mergeWordParts(final Word existingWord, final Word newWord) {
        int nextPosition = existingWord.getWordParts().stream()
                .mapToInt(WordPart::getPosition)
                .max()
                .orElse(0) + 1;

        for (final WordPart newPart : newWord.getWordParts()) {
            final boolean exists = existingWord.getWordParts().stream()
                    .anyMatch(existingPart -> areWordPartsEqual(existingPart, newPart));

            if (!exists) {
                final WordPart wordPart = new WordPart(newPart.getAnswer(), newPart.getBasicWord(), nextPosition++,
                        newPart.getToSpeech(), newPart.getSeparator(), newPart.getSeparatorType(), newPart.getWord());

                existingWord.addWordPart(wordPart);
            }
        }
    }

    @Override
    @Transactional
    public WordDto updateWordCategories(final UUID languageUuid, final UUID wordUuid, final UpdateWordCategoriesForm form) {
        categoryFacade.verifyCategoriesAccess(languageUuid, form.categoryUuids());

        final Word word = wordRepository.findByUuidAndLanguageUuid(wordUuid, languageUuid)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.WORD_NOT_FOUND, wordUuid));

        final List<Category> foundCategories = categoryRepository.findAllByUuidIn(form.categoryUuids());

        if (foundCategories.size() != form.categoryUuids().size()) {
            throw new NotFoundException(ErrorCodes.CATEGORY_NOT_FOUND);
        }

        final Set<Category> newCategories = new HashSet<>(foundCategories);
        final Set<Category> currentCategories = word.getCategories();

        newCategories.stream()
                .filter(category -> !currentCategories.contains(category))
                .forEach(word::addCategory);

        currentCategories.removeIf(category -> !newCategories.contains(category));

        final Word savedWord = wordRepository.save(word);
        return WordMapper.entityToDto(savedWord);
    }

    @Override
    @Transactional
    public void resetWordTime(final UUID languageUuid, final UUID categoryUuid) {
        languageFacade.verifyLanguageOwnership(languageUuid);

        final LocalDateTime now = LocalDateTime.now();

        if (categoryUuid != null) {
            categoryFacade.verifyCategoryAccess(languageUuid, categoryUuid);
            wordRepository.updateResetTimeByCategoryUuid(categoryUuid, now);
        } else {
            wordRepository.updateResetTimeByLanguageUuid(languageUuid, now);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DateStatItem> getWordCreationStatsByDateAndLanguage(final UUID accountUuid,
                                                                    final LocalDate startDate,
                                                                    final LocalDate endDate,
                                                                    final List<UUID> languageUuids) {
        final List<WordStatsProjection> results = wordRepository.findWordCreationStatsByDateAndLanguage(
                accountUuid,
                startDate,
                endDate,
                languageUuids != null && !languageUuids.isEmpty() ? languageUuids : null
        );

        return buildDateStatItems(results, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WordStatsProjection> getAllWordRepeatStatsByDateAndLanguage(final UUID accountUuid,
                                                                            final LocalDate startDate,
                                                                            final LocalDate endDate,
                                                                            final List<UUID> languageUuids) {
        return wordStatsRepository.findWordRepeatStatsByDateAndLanguage(
                accountUuid,
                startDate,
                endDate,
                languageUuids != null && !languageUuids.isEmpty() ? languageUuids : null
        );
    }

    private List<DateStatItem> buildDateStatItems(final List<WordStatsProjection> projections, final Boolean correctFilter) {
        final Map<LocalDate, List<LanguageStatItem>> groupedByDate = new HashMap<>();

        for (final WordStatsProjection projection : projections) {
            if (correctFilter == null || projection.getCorrect().equals(correctFilter)) {
                final LocalDate date = projection.getDate();
                final LanguageStatItem item = new LanguageStatItem(projection.getLanguageUuid(), projection.getCount());

                groupedByDate.computeIfAbsent(date, d -> new ArrayList<>()).add(item);
            }
        }

        return groupedByDate.entrySet().stream()
                .map(entry -> new DateStatItem(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(DateStatItem::date))
                .toList();
    }
}
