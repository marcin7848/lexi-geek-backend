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

    @Override
    public PageDto<CategoryDto> getCategories(final UUID languageUuid, final CategoryFilterForm form, final PageableRequest pageableRequest) {
        pageableRequest.addDefaultSorts(new SortOrder(Category.Fields.order, Sort.Direction.ASC));

        final CategorySpecification specification = new CategorySpecification(form, languageUuid);

        return PageableUtils.toDto(categoryRepository.findAll(specification, PageableUtils.createPageable(pageableRequest))
                .map(CategoryMapper::entityToDto), pageableRequest);
    }

    @Override
    public void createCategory(final UUID languageUuid, final CategoryForm form) {
        final Language language = languageRepository.findByUuid(languageUuid)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.LANGUAGE_NOT_FOUND, languageUuid));

        final Category category = CategoryMapper.formToEntity(form);
        category.setLanguage(language);

        if (form.parentUuid() != null) {
            final Category parent = categoryRepository.findByUuidAndLanguageUuid(form.parentUuid(), languageUuid)
                    .orElseThrow(() -> new NotFoundException(ErrorCodes.PARENT_CATEGORY_NOT_FOUND, form.parentUuid()));
            category.setParent(parent);
        }

        categoryRepository.save(category);
    }

    @Override
    public void editCategory(final UUID languageUuid, final UUID uuid, final CategoryForm form) {
        final Category category = categoryRepository.findByUuidAndLanguageUuid(uuid, languageUuid)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.CATEGORY_NOT_FOUND, uuid));

        CategoryMapper.updateEntityFromForm(category, form);

        if (form.parentUuid() != null) {
            final Category parent = categoryRepository.findByUuidAndLanguageUuid(form.parentUuid(), languageUuid)
                    .orElseThrow(() -> new NotFoundException(ErrorCodes.PARENT_CATEGORY_NOT_FOUND, form.parentUuid()));
            category.setParent(parent);
        } else {
            category.setParent(null);
        }

        categoryRepository.save(category);
    }

    @Override
    @Transactional
    public void deleteCategory(final UUID languageUuid, final UUID uuid) {
        final Category category = categoryRepository.findByUuidAndLanguageUuid(uuid, languageUuid)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.CATEGORY_NOT_FOUND, uuid));
        categoryRepository.delete(category);
    }
}

