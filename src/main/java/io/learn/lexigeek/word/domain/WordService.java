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

        if(category.getMode() == CategoryMode.DICTIONARY) {
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

        final Word word = wordRepository.findByUuidWithDetails(wordUuid)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.WORD_NOT_FOUND, wordUuid));

        WordMapper.updateEntityFromForm(word, form);

        // Update categories if provided
//        if (form.categoryNames() != null) {
//            word.getCategories().clear();
//            final Category mainCategory = categoryRepository.findByUuidAndLanguageUuid(categoryUuid, languageUuid)
//                    .orElseThrow(() -> new NotFoundException(ErrorCodes.CATEGORY_NOT_FOUND, categoryUuid));
//            word.addCategory(mainCategory);
//
//            if (!form.categoryNames().isEmpty()) {
//                final Set<Category> additionalCategories = findCategoriesByNames(languageUuid, form.categoryNames());
//                additionalCategories.forEach(word::addCategory);
//            }
//        }

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
        return WordMapper.entityToDto(savedWord);
    }

    @Override
    @Transactional
    public WordDto updateWordStatus(final UUID languageUuid, final UUID categoryUuid, final UUID wordUuid,
                                    final Boolean accepted, final Boolean chosen, final Boolean toRepeat) {
        categoryFacade.verifyCategoryAccess(languageUuid, categoryUuid);

        final Word word = wordRepository.findByUuidAndCategoryUuid(wordUuid, categoryUuid)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.WORD_NOT_FOUND, wordUuid));

        if (accepted != null) {
            word.setAccepted(accepted);
        }
        if (chosen != null) {
            word.setChosen(chosen);
        }
        if (toRepeat != null) {
            word.setToRepeat(toRepeat);
        }

        final Word savedWord = wordRepository.save(word);
        return WordMapper.entityToDto(savedWord);
    }

    private Set<Category> findCategoriesByNames(final UUID languageUuid, final Set<String> categoryNames) {
        final Set<Category> categories = new HashSet<>();
        // For now, we'll skip finding by names as it would require additional repository methods
        // This can be enhanced later if needed
        return categories;
    }

    /**
     * Finds a word from the existing words list that has matching WordParts with the new word.
     * A match occurs if there's at least one WordPart with the same answer value (true or false)
     * and the same word content.
     */
    private Word findWordWithMatchingParts(final java.util.List<Word> existingWords, final Word newWord) {
        for (Word existingWord : existingWords) {
            if (hasMatchingWordParts(existingWord, newWord)) {
                return existingWord;
            }
        }
        return null;
    }

    /**
     * Checks if two words have matching WordParts.
     * Returns true if there's at least one WordPart in both words with:
     * - Same answer value (both false or both true)
     * - Same word content
     */
    private boolean hasMatchingWordParts(final Word existingWord, final Word newWord) {
        for (WordPart existingPart : existingWord.getWordParts()) {
            for (WordPart newPart : newWord.getWordParts()) {
                if (existingPart.getAnswer().equals(newPart.getAnswer()) &&
                    areWordPartsEqual(existingPart, newPart)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if two WordParts are equal based on their word and answer fields only.
     */
    private boolean areWordPartsEqual(final WordPart part1, final WordPart part2) {
        return java.util.Objects.equals(part1.getWord(), part2.getWord()) &&
               part1.getAnswer().equals(part2.getAnswer());
    }

    /**
     * Merges WordParts from the new word into the existing word.
     * Only adds WordParts that don't already exist in the existing word.
     * Adjusts positions to append new parts at the end.
     */
    private void mergeWordParts(final Word existingWord, final Word newWord) {
        // Calculate the next position for new word parts
        int nextPosition = existingWord.getWordParts().stream()
                .mapToInt(WordPart::getPosition)
                .max()
                .orElse(0) + 1;

        for (WordPart newPart : newWord.getWordParts()) {
            // Check if this WordPart already exists in the existing word
            boolean exists = existingWord.getWordParts().stream()
                    .anyMatch(existingPart -> areWordPartsEqual(existingPart, newPart));

            if (!exists) {
                // Create a new WordPart with updated position
                final WordPart wordPart = new WordPart();
                wordPart.setAnswer(newPart.getAnswer());
                wordPart.setBasicWord(newPart.getBasicWord());
                wordPart.setPosition(nextPosition++);
                wordPart.setToSpeech(newPart.getToSpeech());
                wordPart.setSeparator(newPart.getSeparator());
                wordPart.setSeparatorType(newPart.getSeparatorType());
                wordPart.setWord(newPart.getWord());

                existingWord.addWordPart(wordPart);
            }
        }
    }
}

