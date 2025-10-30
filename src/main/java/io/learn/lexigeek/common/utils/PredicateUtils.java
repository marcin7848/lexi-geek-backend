package io.learn.lexigeek.common.utils;

import io.learn.lexigeek.common.entity.DateRange;
import io.learn.lexigeek.common.dto.RangeForm;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import static java.util.Objects.nonNull;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import jakarta.persistence.criteria.Subquery;
import lombok.experimental.UtilityClass;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@UtilityClass
public class PredicateUtils {

    private static final String LIKE_CHAR = "%";

    public static Predicate buildAndPredicates(final CriteriaBuilder criteriaBuilder,
                                               final Collection<Predicate> predicates) {
        return criteriaBuilder.and(
                predicates.stream()
                        .filter(Objects::nonNull)
                        .toArray(Predicate[]::new)
        );
    }

    public static Predicate buildOrPredicates(final CriteriaBuilder criteriaBuilder,
                                              final Collection<Predicate> predicates) {
        return criteriaBuilder.or(
                predicates.stream()
                        .filter(Objects::nonNull)
                        .toArray(Predicate[]::new)
        );
    }

    public static void addOrPredicates(final Collection<Predicate> predicates,
                                       final CriteriaBuilder criteriaBuilder,
                                       final Collection<Predicate> orPredicates) {
        if (!orPredicates.isEmpty()) {
            predicates.add(buildOrPredicates(criteriaBuilder, orPredicates));
        }
    }

    public static <T, S> void addLikePredicate(final CriteriaBuilder criteriaBuilder,
                                               final List<Predicate> predicates,
                                               final Root<S> root,
                                               final Function<Root<S>, Path<String>> pathFunction,
                                               final T value) {
        if (nonNull(value)) {
            final Path<String> fieldPath = pathFunction.apply(root);
            final String valueString = value.toString();
            if (isNotBlank(valueString)) {
                predicates.add(createLikeCaseInsensitivePredicate(criteriaBuilder, fieldPath, valueString));
            }
        }
    }

    private static Predicate createLikeCaseInsensitivePredicate(final CriteriaBuilder criteriaBuilder,
                                                                final Path<String> path,
                                                                final String value) {
        return criteriaBuilder.like(criteriaBuilder.lower(path), LIKE_CHAR + value.toLowerCase() + LIKE_CHAR);
    }

    public static <T, S> void addEqualPredicate(final CriteriaBuilder criteriaBuilder,
                                                final List<Predicate> predicates,
                                                final Root<S> root,
                                                final Function<Root<S>, Path<T>> pathFunction,
                                                final T value) {
        if (nonNull(value)) {
            final Path<T> fieldPath = pathFunction.apply(root);
            predicates.add(criteriaBuilder.equal(fieldPath, value));
        }
    }

    public static <S> void addEqualIgnoreCasePredicate(final CriteriaBuilder criteriaBuilder,
                                                       final List<Predicate> predicates,
                                                       final Root<S> root,
                                                       final Function<Root<S>, Path<String>> pathFunction,
                                                       final String value) {
        if (nonNull(value)) {
            final Path<String> fieldPath = pathFunction.apply(root);
            predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(fieldPath), value.toLowerCase()));
        }
    }

    public static <T, S> void addIsNullPredicate(final CriteriaBuilder criteriaBuilder,
                                                 final List<Predicate> predicates,
                                                 final Root<S> root,
                                                 final Function<Root<S>, Path<T>> pathFunction) {
        final Path<T> fieldPath = pathFunction.apply(root);
        predicates.add(criteriaBuilder.isNull(fieldPath));
    }

    public static <T extends Enum<T>, S> void addEnumEqualPredicate(final CriteriaBuilder criteriaBuilder,
                                                                    final List<Predicate> predicates,
                                                                    final Root<S> root,
                                                                    final Function<Root<S>, Path<T>> pathFunction,
                                                                    final T value) {
        if (nonNull(value)) {
            final Path<T> fieldPath = pathFunction.apply(root);
            predicates.add(criteriaBuilder.equal(fieldPath, value));
        }
    }

    public static <T, S> void addInPredicate(final Collection<Predicate> predicates,
                                             final Root<S> root,
                                             final Function<Root<S>, Path<T>> pathFunction,
                                             final Collection<T> values) {
        if (isNotEmpty(values)) {
            final Path<T> fieldPath = pathFunction.apply(root);
            predicates.add(fieldPath.in(values));
        }
    }

    public static <T, S, U, V> void addExistsSubqueryPredicate(final Collection<Predicate> predicates,
                                                               final CriteriaBuilder criteriaBuilder,
                                                               final CriteriaQuery<?> query,
                                                               final Root<S> root,
                                                               final Collection<U> values,
                                                               final Class<T> subqueryEntityClass,
                                                               final Class<V> subqueryReturnType,
                                                               final Function<Root<T>, Expression<V>> selectExpressionFunction,
                                                               final BiFunction<Root<T>, Root<S>, Predicate[]> whereConditionsFunction) {
        if (isNotEmpty(values)) {
            final Subquery<V> subquery = query.subquery(subqueryReturnType);
            final Root<T> subRoot = subquery.from(subqueryEntityClass);

            subquery.select(selectExpressionFunction.apply(subRoot))
                    .where(whereConditionsFunction.apply(subRoot, root));

            predicates.add(criteriaBuilder.exists(subquery));
        }
    }

    public static <T extends Comparable<? super T>, S> void addRangePredicate(final CriteriaBuilder criteriaBuilder,
                                                                              final List<Predicate> predicates,
                                                                              final Root<S> root,
                                                                              final Function<Root<S>, Path<T>> pathFunction,
                                                                              final RangeForm<T> range) {
        if (nonNull(range)) {
            final Path<T> fieldPath = pathFunction.apply(root);
            if (nonNull(range.min())) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(fieldPath, range.min()));
            }

            if (nonNull(range.max())) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(fieldPath, range.max()));
            }
        }
    }

    public static <T extends Comparable<? super T>, S> void addDateRangePredicate(final CriteriaBuilder criteriaBuilder,
                                                                                  final List<Predicate> predicates,
                                                                                  final Root<S> root,
                                                                                  final Function<Root<S>, Path<LocalDate>> pathFunction,
                                                                                  final String dateRange) {
        if (nonNull(dateRange)) {
            final Path<LocalDate> fieldPath = pathFunction.apply(root);
            final Set<DateRange> dateRanges = DateTimeUtils.fromRangeStringToDateRange(dateRange);
            final List<Predicate> subPredicates = dateRanges.stream()
                    .map(date -> nonNull(date.max())
                            ? criteriaBuilder.between(fieldPath, date.min(), date.max())
                            : criteriaBuilder.greaterThanOrEqualTo(fieldPath, date.min())
                    )
                    .toList();
            addOrPredicates(predicates, criteriaBuilder, subPredicates);
        }
    }
}
