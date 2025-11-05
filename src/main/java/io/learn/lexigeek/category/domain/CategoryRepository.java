package io.learn.lexigeek.category.domain;

import io.learn.lexigeek.common.repository.UUIDAwareJpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
interface CategoryRepository extends UUIDAwareJpaRepository<Category, Long>, JpaSpecificationExecutor<Category> {
    Optional<Category> findByUuidAndLanguageUuid(final UUID uuid, final UUID languageUuid);

    @Query("SELECT COALESCE(MAX(c.position), -1) FROM Category c WHERE c.language.uuid = :languageUuid")
    Integer findMaxPositionByLanguageUuid(@Param("languageUuid") UUID languageUuid);

    @Modifying
    @Query("UPDATE Category c SET c.position = c.position - 1 WHERE c.language.uuid = :languageUuid " +
            "AND c.position > :position AND c.uuid != :excludeUuid")
    void decrementPositionsAfter(@Param("languageUuid") UUID languageUuid,
                                  @Param("position") Integer position,
                                  @Param("excludeUuid") UUID excludeUuid);

    @Modifying
    @Query("UPDATE Category c SET c.position = c.position + 1 WHERE c.language.uuid = :languageUuid " +
            "AND c.position >= :position AND c.uuid != :excludeUuid")
    void incrementPositionsFrom(@Param("languageUuid") UUID languageUuid,
                                @Param("position") Integer position,
                                @Param("excludeUuid") UUID excludeUuid);

    @Query("SELECT c FROM Category c WHERE c.uuid = :uuid AND c.language.uuid = :languageUuid")
    Optional<Category> findByUuidAndLanguageUuidWithParent(@Param("uuid") UUID uuid,
                                                            @Param("languageUuid") UUID languageUuid);

    @Query("SELECT c FROM Category c WHERE c.parent.uuid = :parentUuid")
    List<Category> findByParentUuid(@Param("parentUuid") UUID parentUuid);

    @Modifying
    @Query("UPDATE Category c SET c.position = c.position + 1 WHERE c.language.uuid = :languageUuid " +
            "AND c.position >= :fromPosition AND c.position < :toPosition AND c.uuid != :excludeUuid")
    void incrementPositionsBetween(@Param("languageUuid") UUID languageUuid,
                                    @Param("fromPosition") Integer fromPosition,
                                    @Param("toPosition") Integer toPosition,
                                    @Param("excludeUuid") UUID excludeUuid);

    @Modifying
    @Query("UPDATE Category c SET c.position = c.position - 1 WHERE c.language.uuid = :languageUuid " +
            "AND c.position > :fromPosition AND c.position <= :toPosition AND c.uuid != :excludeUuid")
    void decrementPositionsBetween(@Param("languageUuid") UUID languageUuid,
                                    @Param("fromPosition") Integer fromPosition,
                                    @Param("toPosition") Integer toPosition,
                                    @Param("excludeUuid") UUID excludeUuid);
}
