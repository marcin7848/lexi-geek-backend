package io.learn.lexigeek.category.domain;

import io.learn.lexigeek.common.repository.UUIDAwareJpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
interface CategoryRepository extends UUIDAwareJpaRepository<Category, Long>, JpaSpecificationExecutor<Category> {
    Optional<Category> findByUuidAndLanguageUuid(final UUID uuid, final UUID languageUuid);

    @Query("""
            SELECT COALESCE(MAX(c.position), -1) FROM Category c WHERE c.language.uuid = :languageUuid
            """)
    Integer findMaxPositionByLanguageUuid(@Param("languageUuid") final UUID languageUuid);

    @Modifying
    @Query("""
            UPDATE Category c SET c.position = c.position + 1 WHERE c.language.uuid = :languageUuid
                        AND c.position >= :fromPosition AND c.position < :toPosition AND c.uuid != :excludeUuid
            """)
    void incrementPositionsBetween(@Param("languageUuid") final UUID languageUuid,
                                   @Param("fromPosition") final Integer fromPosition,
                                   @Param("toPosition") final Integer toPosition,
                                   @Param("excludeUuid") final UUID excludeUuid);

    @Modifying
    @Query("""
            UPDATE Category c SET c.position = c.position - 1 WHERE c.language.uuid = :languageUuid
                        AND c.position > :fromPosition AND c.position <= :toPosition AND c.uuid != :excludeUuid
            """)
    void decrementPositionsBetween(@Param("languageUuid") final UUID languageUuid,
                                   @Param("fromPosition") final Integer fromPosition,
                                   @Param("toPosition") final Integer toPosition,
                                   @Param("excludeUuid") final UUID excludeUuid);

    @Modifying
    @Query("""
            UPDATE Category c SET c.position = c.position - 1 WHERE c.language.uuid = :languageUuid 
                        AND c.position > :deletedPosition
            """)
    void decrementPositionsAfter(@Param("languageUuid") final UUID languageUuid,
                                 @Param("deletedPosition") final Integer deletedPosition);

    @Query(value = """
            WITH RECURSIVE parent_hierarchy AS (
                     SELECT id, uuid, parent_id, 0 as depth
                     FROM categories
                     WHERE uuid = CAST(:startUuid AS uuid)
                     UNION ALL
                     SELECT c.id, c.uuid, c.parent_id, ph.depth + 1
                     FROM categories c
                     INNER JOIN parent_hierarchy ph ON c.id = ph.parent_id
                     WHERE ph.depth < 100
                     )
                     SELECT COUNT(*) > 0
                     FROM parent_hierarchy
                     WHERE uuid = CAST(:targetUuid AS uuid)
                     AND depth > 0
            """, nativeQuery = true)
    boolean isInParentHierarchy(@Param("startUuid") final String startUuid, @Param("targetUuid") final String targetUuid);
}
