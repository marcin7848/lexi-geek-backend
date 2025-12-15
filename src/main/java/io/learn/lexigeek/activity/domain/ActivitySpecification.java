package io.learn.lexigeek.activity.domain;

import io.learn.lexigeek.activity.dto.ActivityFilterForm;
import io.learn.lexigeek.common.dto.DateTimeRangeForm;
import io.learn.lexigeek.common.entity.AbstractEntity;
import jakarta.persistence.criteria.*;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static io.learn.lexigeek.common.utils.PredicateUtils.addEqualPredicate;
import static io.learn.lexigeek.common.utils.PredicateUtils.buildAndPredicates;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class ActivitySpecification implements Specification<Activity> {

    private final transient ActivityFilterForm form;
    private final transient Long accountId;

    @Override
    public Predicate toPredicate(@Nullable final Root<Activity> root,
                                 @NonNull final CriteriaQuery<?> query,
                                 @NonNull final CriteriaBuilder criteriaBuilder) {
        final List<Predicate> predicates = new ArrayList<>();

        addEqualPredicate(criteriaBuilder, predicates, root, r -> r.get(Activity.Fields.account).get(AbstractEntity.Fields.id), accountId);

        if (form.type() != null) {
            addEqualPredicate(criteriaBuilder, predicates, root, r -> r.get(Activity.Fields.type), form.type());
        }

        if (form.range() != null) {
            final DateTimeRangeForm timestampRange = DateTimeRangeForm.convertToUtc(form.range());
            final Path<LocalDateTime> createdPath = root.get(Activity.Fields.created);

            if (timestampRange.min() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(createdPath, timestampRange.min()));
            }

            if (timestampRange.max() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(createdPath, timestampRange.max()));
            }
        }

        return buildAndPredicates(criteriaBuilder, predicates);
    }
}
