package io.learn.lexigeek.category.domain;

import io.learn.lexigeek.category.CategoryFacade;
import io.learn.lexigeek.category.dto.CategoryDto;
import io.learn.lexigeek.category.dto.CategoryFilterForm;
import io.learn.lexigeek.category.dto.CategoryForm;
import io.learn.lexigeek.category.dto.UpdateCategoryPositionForm;
import io.learn.lexigeek.common.exception.NotFoundException;
import io.learn.lexigeek.common.exception.ValidationException;
import io.learn.lexigeek.common.pageable.PageDto;
import io.learn.lexigeek.common.pageable.PageableRequest;
import io.learn.lexigeek.common.pageable.PageableUtils;
import io.learn.lexigeek.common.pageable.SortOrder;
import io.learn.lexigeek.common.validation.ErrorCodes;
import io.learn.lexigeek.language.LanguageFacade;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class CategoryService implements CategoryFacade {

    private final CategoryRepository categoryRepository;
    private final LanguageRepository languageRepository;
    private final LanguageFacade languageFacade;

    @Override
    public PageDto<CategoryDto> getCategories(final UUID languageUuid, final CategoryFilterForm form, final PageableRequest pageableRequest) {
        languageFacade.verifyLanguageOwnership(languageUuid);

        pageableRequest.addDefaultSorts(new SortOrder(Category.Fields.position, Sort.Direction.ASC));

        final CategorySpecification specification = new CategorySpecification(form, languageUuid);

        return PageableUtils.toDto(categoryRepository.findAll(specification, PageableUtils.createPageable(pageableRequest))
                .map(CategoryMapper::entityToDto), pageableRequest);
    }

    @Override
    @Transactional
    public void createCategory(final UUID languageUuid, final CategoryForm form) {
        languageFacade.verifyLanguageOwnership(languageUuid);

        final Language language = languageRepository.findByUuid(languageUuid)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.LANGUAGE_NOT_FOUND, languageUuid));

        final Category category = CategoryMapper.formToEntity(form);
        category.setLanguage(language);

        setParentIfProvided(languageUuid, form, category);

        // Find max position globally across all categories in language
        final Integer maxPosition = categoryRepository.findMaxPositionByLanguageUuid(languageUuid);
        category.setPosition(maxPosition + 1);

        categoryRepository.save(category);
    }

    @Override
    @Transactional
    public void editCategory(final UUID languageUuid, final UUID uuid, final CategoryForm form) {
        languageFacade.verifyLanguageOwnership(languageUuid);

        final Category category = categoryRepository.findByUuidAndLanguageUuid(uuid, languageUuid)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.CATEGORY_NOT_FOUND, uuid));

        CategoryMapper.updateEntityFromForm(category, form);

        setParentIfProvided(languageUuid, form, category);

        categoryRepository.save(category);
    }

    private void setParentIfProvided(final UUID languageUuid, final CategoryForm form, final Category category) {
        if (form.parentUuid() != null) {
            final Category parent = categoryRepository.findByUuidAndLanguageUuid(form.parentUuid(), languageUuid)
                    .orElseThrow(() -> new NotFoundException(ErrorCodes.PARENT_CATEGORY_NOT_FOUND, form.parentUuid()));
            category.setParent(parent);
        } else {
            category.setParent(null);
        }
    }

    @Override
    @Transactional
    public void deleteCategory(final UUID languageUuid, final UUID uuid) {
        languageFacade.verifyLanguageOwnership(languageUuid);

        final Category category = categoryRepository.findByUuidAndLanguageUuid(uuid, languageUuid)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.CATEGORY_NOT_FOUND, uuid));
        categoryRepository.delete(category);
    }

    @Override
    @Transactional
    public void updateCategoryPosition(final UUID languageUuid, final UUID uuid, final UpdateCategoryPositionForm form) {
        languageFacade.verifyLanguageOwnership(languageUuid);

        final Category category = categoryRepository.findByUuidAndLanguageUuid(uuid, languageUuid)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.CATEGORY_NOT_FOUND, uuid));

        final Integer oldPosition = category.getPosition();
        final UUID newParentUuid = form.parentUuid();
        final Integer newPosition = form.position();

        // Check if this is a no-op (same position)
        if (oldPosition.equals(newPosition)) {
            // Even if parent changes, if position stays the same, we might still need to update parent
            // But no position reordering needed
            if (newParentUuid != null) {
                final Category newParent = categoryRepository.findByUuidAndLanguageUuid(newParentUuid, languageUuid)
                        .orElseThrow(() -> new NotFoundException(ErrorCodes.PARENT_CATEGORY_NOT_FOUND, newParentUuid));

                if (wouldCreateCircularReference(uuid, newParentUuid)) {
                    throw new ValidationException(ErrorCodes.CIRCULAR_REFERENCE_ERROR, "Cannot move category to its own descendant");
                }

                category.setParent(newParent);
            } else {
                category.setParent(null);
            }
            categoryRepository.save(category);
            return;
        }

        // Validate parent if provided
        Category newParent = null;
        if (newParentUuid != null) {
            newParent = categoryRepository.findByUuidAndLanguageUuid(newParentUuid, languageUuid)
                    .orElseThrow(() -> new NotFoundException(ErrorCodes.PARENT_CATEGORY_NOT_FOUND, newParentUuid));

            // Check for circular reference
            if (wouldCreateCircularReference(uuid, newParentUuid)) {
                throw new ValidationException(ErrorCodes.CIRCULAR_REFERENCE_ERROR, "Cannot move category to its own descendant");
            }
        }

        // Global position reordering (ignoring parent context)
        if (newPosition < oldPosition) {
            // Moving up (e.g., from position 3 to position 1)
            // All categories at positions [newPosition, oldPosition) shift up by 1
            categoryRepository.incrementPositionsBetween(languageUuid, newPosition, oldPosition, uuid);
        } else {
            // Moving down (e.g., from position 1 to position 3)
            // All categories at positions (oldPosition, newPosition] shift down by 1
            categoryRepository.decrementPositionsBetween(languageUuid, oldPosition, newPosition, uuid);
        }

        category.setParent(newParent);
        category.setPosition(newPosition);
        categoryRepository.save(category);
    }

    private boolean wouldCreateCircularReference(final UUID categoryUuid, final UUID newParentUuid) {
        UUID currentParentUuid = newParentUuid;
        int depth = 0;
        final int maxDepth = 100;

        while (currentParentUuid != null && depth < maxDepth) {
            if (currentParentUuid.equals(categoryUuid)) {
                return true;
            }

            final Optional<Category> parentCategory = categoryRepository.findByUuid(currentParentUuid);
            if (parentCategory.isEmpty()) {
                break;
            }

            currentParentUuid = parentCategory.get().getParent() != null
                    ? parentCategory.get().getParent().getUuid()
                    : null;
            depth++;
        }

        return false;
    }
}
