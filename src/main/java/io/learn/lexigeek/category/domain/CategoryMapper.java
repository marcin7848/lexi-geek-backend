package io.learn.lexigeek.category.domain;

import io.learn.lexigeek.category.dto.CategoryDto;
import io.learn.lexigeek.category.dto.CategoryForm;
import lombok.experimental.UtilityClass;

@UtilityClass
class CategoryMapper {

    Category formToEntity(final CategoryForm form) {
        final Category category = new Category();
        updateEntityFromForm(category, form);
        return category;
    }

    void updateEntityFromForm(final Category category, final CategoryForm form) {
        category.setMode(form.mode());
        category.setMethod(form.method());
        category.setOrder(form.order());
    }

    CategoryDto entityToDto(final Category category) {
        return new CategoryDto(
                category.getUuid(),
                category.getParent() != null ? category.getParent().getUuid() : null,
                category.getMode(),
                category.getMethod(),
                category.getOrder());
    }
}

