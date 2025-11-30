package io.learn.lexigeek.language.domain;

import io.learn.lexigeek.common.entity.AbstractEntity;
import io.learn.lexigeek.common.entity.AbstractUuidEntity;
import io.learn.lexigeek.language.dto.LanguageFilterForm;
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

import static io.learn.lexigeek.common.utils.PredicateUtils.addEqualPredicate;
import static io.learn.lexigeek.common.utils.PredicateUtils.addLikePredicate;
import static io.learn.lexigeek.common.utils.PredicateUtils.buildAndPredicates;


@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class LanguageSpecification implements Specification<Language> {

    private final transient LanguageFilterForm form;
    private final transient Long accountId;

    @Override
    public Predicate toPredicate(@Nullable final Root<Language> root,
                                 @NonNull final CriteriaQuery<?> query,
                                 @NonNull final CriteriaBuilder criteriaBuilder) {
        final List<Predicate> predicates = new ArrayList<>();

        addEqualPredicate(criteriaBuilder, predicates, root, r -> r.get(Language.Fields.account).get(AbstractEntity.Fields.id), accountId);

        addEqualPredicate(criteriaBuilder, predicates, root, r -> r.get(AbstractUuidEntity.Fields.uuid), form.uuid());
        addLikePredicate(criteriaBuilder, predicates, root, r -> r.get(Language.Fields.name), form.name());
        addLikePredicate(criteriaBuilder, predicates, root, r -> r.get(Language.Fields.shortcut), form.shortcut());
        addLikePredicate(criteriaBuilder, predicates, root, r -> r.get(Language.Fields.codeForSpeech), form.codeForSpeech());
        addLikePredicate(criteriaBuilder, predicates, root, r -> r.get(Language.Fields.codeForTranslator), form.codeForTranslator());
        addEqualPredicate(criteriaBuilder, predicates, root, r -> r.get(Language.Fields.isPublic), form.isPublic());
        addLikePredicate(criteriaBuilder, predicates, root, r -> r.get(Language.Fields.specialLetters), form.specialLetters());

        return buildAndPredicates(criteriaBuilder, predicates);
    }
}
