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
    private final transient UUID languageUuid;
    private final transient Long currentAccountId;

    @Override
    public Predicate toPredicate(@Nullable final Root<Word> root,
                                 @NonNull final CriteriaQuery<?> query,
                                 @NonNull final CriteriaBuilder criteriaBuilder) {
        final List<Predicate> predicates = new ArrayList<>();

        // Only accepted words (public words)
        predicates.add(criteriaBuilder.isTrue(root.get(Word.Fields.accepted)));

        // Join with categories and languages to apply filters
        final Join<Object, Object> categoryJoin = root.join(Word.Fields.categories, JoinType.INNER);
        final Join<Object, Object> languageJoin = categoryJoin.join("language", JoinType.INNER);

        // Only fetch words from public languages
        predicates.add(criteriaBuilder.isTrue(languageJoin.get(Language.Fields.isPublic)));

        // Only fetch words from languages with the same shortcut as the requested language
        if (languageUuid != null) {
            final Subquery<String> languageShortcutSubquery = query.subquery(String.class);
            final Root<Language> requestedLanguageRoot = languageShortcutSubquery.from(Language.class);
            languageShortcutSubquery.select(requestedLanguageRoot.get(Language.Fields.shortcut))
                    .where(criteriaBuilder.equal(requestedLanguageRoot.get(AbstractUuidEntity.Fields.uuid), languageUuid));

            predicates.add(criteriaBuilder.equal(
                    languageJoin.get(Language.Fields.shortcut),
                    languageShortcutSubquery
            ));
        }

        // Only fetch words from other users (not from current user's languages)
        final Subquery<Long> currentUserLanguageSubquery = query.subquery(Long.class);
        final Root<Language> userLanguageRoot = currentUserLanguageSubquery.from(Language.class);
        currentUserLanguageSubquery.select(userLanguageRoot.get(AbstractEntity.Fields.id))
                .where(criteriaBuilder.equal(
                        userLanguageRoot.get(Language.Fields.account).get(AbstractEntity.Fields.id),
                        currentAccountId
                ));

        predicates.add(criteriaBuilder.not(criteriaBuilder.in(languageJoin.get(AbstractEntity.Fields.id)).value(currentUserLanguageSubquery)));

        // Filter by mechanism (skip if null, don't filter for ALL)
        if (form.mechanism() != null) {
            addEqualPredicate(criteriaBuilder, predicates, root, r -> r.get(Word.Fields.mechanism), form.mechanism());
        }

        // Filter by search text (searches in comment or word parts)
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

        // Filter by category name (uses LIKE for partial matching)
        if (form.categoryName() != null && !form.categoryName().isBlank()) {
            predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(categoryJoin.get("name")),
                    "%" + form.categoryName().toLowerCase() + "%"
            ));
        }

        query.distinct(true);

        return buildAndPredicates(criteriaBuilder, predicates);
    }
}

