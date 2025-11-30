package io.learn.lexigeek.word.domain;

import io.learn.lexigeek.common.entity.AbstractEntity;
import io.learn.lexigeek.common.entity.AbstractUuidEntity;
import io.learn.lexigeek.word.dto.PublicWordFilterForm;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
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
class PublicWordSpecification implements Specification<Word> {

    private final transient PublicWordFilterForm form;
    private final transient UUID categoryUuid;
    private final transient Long currentAccountId;

    @Override
    public Predicate toPredicate(@Nullable final Root<Word> root,
                                 @NonNull final CriteriaQuery<?> query,
                                 @NonNull final CriteriaBuilder criteriaBuilder) {
        final List<Predicate> predicates = new ArrayList<>();

        // Only accepted words (public words)
        predicates.add(criteriaBuilder.isTrue(root.get(Word.Fields.accepted)));

        // Words from other users (not created by current user's categories)
        // Check if word exists in any of current user's categories
        if (categoryUuid != null) {
            final Subquery<Long> userCategorySubquery = query.subquery(Long.class);
            final Root<Word> userWordRoot = userCategorySubquery.from(Word.class);
            final Join<Word, Category> userCategoryJoin = userWordRoot.join(Word.Fields.categories, JoinType.INNER);
            final Join<Category, Language> userLanguageJoin = userCategoryJoin.join("language", JoinType.INNER);

            userCategorySubquery.select(userWordRoot.get(AbstractEntity.Fields.id))
                    .where(criteriaBuilder.and(
                            criteriaBuilder.equal(userWordRoot.get(AbstractEntity.Fields.id), root.get(AbstractEntity.Fields.id)),
                            criteriaBuilder.equal(userCategoryJoin.get(AbstractUuidEntity.Fields.uuid), categoryUuid)
                    ));

            predicates.add(criteriaBuilder.not(criteriaBuilder.exists(userCategorySubquery)));
        }

        // Exclude viewed words
        final Subquery<Long> viewedSubquery = query.subquery(Long.class);
        final Root<ViewedPublicWord> viewedRoot = viewedSubquery.from(ViewedPublicWord.class);
        viewedSubquery.select(viewedRoot.get(ViewedPublicWord.Fields.word).get(AbstractEntity.Fields.id))
                .where(criteriaBuilder.and(
                        criteriaBuilder.equal(viewedRoot.get(ViewedPublicWord.Fields.word).get(AbstractEntity.Fields.id), root.get(AbstractEntity.Fields.id)),
                        criteriaBuilder.equal(viewedRoot.get(ViewedPublicWord.Fields.accountId), currentAccountId)
                ));

        predicates.add(criteriaBuilder.not(criteriaBuilder.exists(viewedSubquery)));

        // Filter by mechanism
        addEqualPredicate(criteriaBuilder, predicates, root, r -> r.get(Word.Fields.mechanism), form.mechanism());

        // Filter by search text
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

        // Filter by category name
        if (form.categoryName() != null && !form.categoryName().isBlank()) {
            final Join<Word, Category> categoryJoin = root.join(Word.Fields.categories, JoinType.INNER);
            predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(categoryJoin.get("name")),
                    "%" + form.categoryName().toLowerCase() + "%"
            ));
        }

        query.distinct(true);

        return buildAndPredicates(criteriaBuilder, predicates);
    }
}

