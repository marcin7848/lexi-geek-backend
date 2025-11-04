package io.learn.lexigeek.category.domain;

import io.learn.lexigeek.common.repository.UUIDAwareJpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
interface CategoryRepository extends UUIDAwareJpaRepository<Category, Long>, JpaSpecificationExecutor<Category> {
    Optional<Category> findByUuidAndLanguageUuid(final UUID uuid, final UUID languageUuid);
}

