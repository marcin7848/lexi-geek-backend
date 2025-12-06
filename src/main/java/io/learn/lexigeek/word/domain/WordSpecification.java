package io.learn.lexigeek.word.domain;

import io.learn.lexigeek.common.entity.AbstractUuidEntity;
import io.learn.lexigeek.word.dto.WordFilterForm;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
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
class WordSpecification implements Specification<Word> {

    private final transient WordFilterForm form;
    private final transient UUID categoryUuid;

    @Override
    public Predicate toPredicate(@Nullable final Root<Word> root,
                                 @NonNull final CriteriaQuery<?> query,
                                 @NonNull final CriteriaBuilder criteriaBuilder) {
        final List<Predicate> predicates = new ArrayList<>();

        if (categoryUuid != null) {
            final Join<Object, Object> categoriesJoin = root.join(Word.Fields.categories, JoinType.INNER);
            predicates.add(criteriaBuilder.equal(
                    categoriesJoin.get(AbstractUuidEntity.Fields.uuid),
                    categoryUuid
            ));
        }

        addEqualPredicate(criteriaBuilder, predicates, root, r -> r.get(AbstractUuidEntity.Fields.uuid), form.uuid());
        addEqualPredicate(criteriaBuilder, predicates, root, r -> r.get(Word.Fields.accepted), form.accepted());
        addEqualPredicate(criteriaBuilder, predicates, root, r -> r.get(Word.Fields.chosen), form.chosen());
        addEqualPredicate(criteriaBuilder, predicates, root, r -> r.get(Word.Fields.mechanism), form.mechanism());

        if (form.searchText() != null && !form.searchText().isBlank()) {
            final String searchPattern = "%" + form.searchText().toLowerCase() + "%";
            final Predicate commentPredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get(Word.Fields.comment)),
                    searchPattern
            );

            final Join<Word, WordPart> wordPartsJoin = root.join(Word.Fields.wordParts, JoinType.LEFT);
            final Predicate wordPartPredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(wordPartsJoin.get(WordPart.Fields.word)),
                    searchPattern
            );

            predicates.add(criteriaBuilder.or(commentPredicate, wordPartPredicate));
        }

        query.distinct(true);

        return buildAndPredicates(criteriaBuilder, predicates);
    }
}

