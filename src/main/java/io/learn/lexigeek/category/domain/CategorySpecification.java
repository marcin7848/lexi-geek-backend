package io.learn.lexigeek.category.domain;

import io.learn.lexigeek.category.dto.CategoryFilterForm;
import io.learn.lexigeek.common.entity.AbstractUuidEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.learn.lexigeek.common.utils.PredicateUtils.addEqualPredicate;
import static io.learn.lexigeek.common.utils.PredicateUtils.buildAndPredicates;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class CategorySpecification implements Specification<Category> {

    private final transient CategoryFilterForm form;
    private final transient UUID languageUuid;

    @Override
    public Predicate toPredicate(@Nullable final Root<Category> root,
                                 @NonNull final CriteriaQuery<?> query,
                                 @NonNull final CriteriaBuilder criteriaBuilder) {
        final List<Predicate> predicates = new ArrayList<>();

        addEqualPredicate(criteriaBuilder, predicates, root, r -> r.get(Category.Fields.language).get(AbstractUuidEntity.Fields.uuid), languageUuid);

        addEqualPredicate(criteriaBuilder, predicates, root, r -> r.get(AbstractUuidEntity.Fields.uuid), form.uuid());
        addEqualPredicate(criteriaBuilder, predicates, root, r -> r.get(Category.Fields.parent).get(AbstractUuidEntity.Fields.uuid), form.parentUuid());
        addEqualPredicate(criteriaBuilder, predicates, root, r -> r.get(Category.Fields.mode), form.mode());
        addEqualPredicate(criteriaBuilder, predicates, root, r -> r.get(Category.Fields.method), form.method());
        addEqualPredicate(criteriaBuilder, predicates, root, r -> r.get(Category.Fields.order), form.order());

        return buildAndPredicates(criteriaBuilder, predicates);
    }
}

