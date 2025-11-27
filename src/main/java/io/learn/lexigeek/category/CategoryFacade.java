package io.learn.lexigeek.category;

import io.learn.lexigeek.category.dto.CategoryDto;
import io.learn.lexigeek.category.dto.CategoryFilterForm;
import io.learn.lexigeek.category.dto.CategoryForm;
import io.learn.lexigeek.category.dto.UpdateCategoryPositionForm;
import io.learn.lexigeek.common.pageable.PageDto;
import io.learn.lexigeek.common.pageable.PageableRequest;

import java.util.List;
import java.util.UUID;

public interface CategoryFacade {

    PageDto<CategoryDto> getCategories(final UUID languageUuid, final CategoryFilterForm form, final PageableRequest pageableRequest);

    void createCategory(final UUID languageUuid, final CategoryForm form);

    void editCategory(final UUID languageUuid, final UUID uuid, final CategoryForm form);

    void deleteCategory(final UUID languageUuid, final UUID uuid);

    void updateCategoryPosition(final UUID languageUuid, final UUID uuid, final UpdateCategoryPositionForm form);

    void verifyCategoryAccess(final UUID languageUuid, final UUID categoryUuid);

    void verifyCategoriesAccess(final UUID languageUuid, final List<UUID> categoryUuids);
}
