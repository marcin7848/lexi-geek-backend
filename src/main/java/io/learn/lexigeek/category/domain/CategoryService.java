package io.learn.lexigeek.category.domain;

import io.learn.lexigeek.category.CategoryFacade;
import io.learn.lexigeek.category.dto.CategoryDto;
import io.learn.lexigeek.category.dto.CategoryFilterForm;
import io.learn.lexigeek.category.dto.CategoryForm;
import io.learn.lexigeek.common.exception.NotFoundException;
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
}

