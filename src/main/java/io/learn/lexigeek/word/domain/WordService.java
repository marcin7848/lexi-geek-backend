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
import io.learn.lexigeek.word.WordFacade;
import io.learn.lexigeek.word.dto.UpdateWordCategoriesForm;
import io.learn.lexigeek.word.dto.WordDto;
import io.learn.lexigeek.word.dto.WordFilterForm;
import io.learn.lexigeek.word.dto.WordForm;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static java.util.stream.Collectors.toSet;

@Service
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class WordService implements WordFacade {

    private final WordRepository wordRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryFacade categoryFacade;

    @Override
    public PageDto<WordDto> getWords(final UUID languageUuid, final UUID categoryUuid,
                                     final WordFilterForm form, final PageableRequest pageableRequest) {
        pageableRequest.addDefaultSorts(new SortOrder(Word.Fields.created, Sort.Direction.DESC));

        categoryFacade.verifyCategoryAccess(languageUuid, categoryUuid);

        final WordSpecification specification = new WordSpecification(form, categoryUuid);

        // Check if sorting by computed fields (lastTimeRepeated or repeated)
        final boolean sortByComputedField = pageableRequest.getSortBy().stream()
                .anyMatch(sort -> "lastTimeRepeated".equals(sort.getField()) || "repeated".equals(sort.getField()));

        if (sortByComputedField) {
            // Load all words without pagination, compute fields, then sort and paginate in memory
            final List<Word> allWords = wordRepository.findAll(specification);
            final List<WordDto> wordDtos = allWords.stream()
                    .map(WordMapper::entityToDto)
                    .toList();

            final Comparator<WordDto> comparator = getWordDtoComparator(pageableRequest);
            final long total = wordDtos.size();

            return PageableUtils.listToPageDto(wordDtos, pageableRequest, total, comparator);
        }

        return PageableUtils.toDto(wordRepository.findAll(specification, PageableUtils.createPageable(pageableRequest))
                .map(WordMapper::entityToDto), pageableRequest);
    }

    private Comparator<WordDto> getWordDtoComparator(final PageableRequest pageableRequest) {
        final SortOrder sortOrder = pageableRequest.getFirstSort();
        if (sortOrder == null || sortOrder.getField() == null) {
            return (o1, o2) -> 0;
        }

        // Return ASC comparator - PageableUtils.sort() will handle reversing for DESC
        return switch (sortOrder.getField()) {
            case "lastTimeRepeated" -> Comparator.comparing(WordDto::lastTimeRepeated,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            case "repeated" -> Comparator.comparing(WordDto::repeated,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            default -> (o1, o2) -> 0;
        };
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

            final List<Word> existingWords = wordRepository.findByCategoryUuidsWithDetails(categories);

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
    public WordDto acceptWord(final UUID languageUuid, final UUID categoryUuid, final UUID wordUuid) {
        categoryFacade.verifyCategoryAccess(languageUuid, categoryUuid);

        final Word word = wordRepository.findByUuidAndCategoryUuid(wordUuid, categoryUuid)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.WORD_NOT_FOUND, wordUuid));

        word.setAccepted(true);
        final Word savedWord = wordRepository.save(word);
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
}
