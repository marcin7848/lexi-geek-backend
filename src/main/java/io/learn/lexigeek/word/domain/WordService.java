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
        word.addCategory(category);

        final Set<UUID> categories = categoryFacade.getCategories(
                        languageUuid,
                        new CategoryFilterForm(null, null, null, CategoryMode.DICTIONARY, null, null),
                        PageableRequest.builder().singlePage(true).build())
                .getItems().stream()
                .map(CategoryDto::uuid)
                .collect(toSet());

        //TODO: zanim doda nowe slowo sprawdź czy jakiś wordPart w nowym słowie jest answer i jest dla category DICTIONARY
        // który pokrywa się z innym słowiem (wordPart answer true, DISCOTINARY) -> jesli tak, połącz wordParts, dodaj na koniec i zrób słowo accepted na false

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
}

