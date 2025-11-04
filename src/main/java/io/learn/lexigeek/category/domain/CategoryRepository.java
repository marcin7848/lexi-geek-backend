package io.learn.lexigeek.category.domain;

import io.learn.lexigeek.common.repository.UUIDAwareJpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
interface CategoryRepository extends UUIDAwareJpaRepository<Category, Long>, JpaSpecificationExecutor<Category> {
    Optional<Category> findByUuidAndLanguageUuid(final UUID uuid, final UUID languageUuid);

    @Query("SELECT COALESCE(MAX(c.position), 0) FROM Category c WHERE c.language.uuid = :languageUuid")
    Integer findMaxPositionByLanguageUuid(@Param("languageUuid") UUID languageUuid);
}

