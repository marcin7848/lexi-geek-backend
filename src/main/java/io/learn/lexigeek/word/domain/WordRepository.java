package io.learn.lexigeek.word.domain;

import io.learn.lexigeek.common.repository.UUIDAwareJpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
interface WordRepository extends UUIDAwareJpaRepository<Word, Long>, JpaSpecificationExecutor<Word> {

    Optional<Word> findByUuidAndCategories(final UUID uuid, final Set<Category> category);

    @Query("""
            SELECT w FROM Word w
                        LEFT JOIN FETCH w.wordParts wp
                        LEFT JOIN FETCH w.categories c
                        WHERE w.uuid = :uuid
                        AND EXISTS (SELECT 1 FROM w.categories cat WHERE cat.uuid = :categoryUuid)
            """)
    Optional<Word> findByUuidAndCategoryUuid(@Param("uuid") final UUID uuid,
                                             @Param("categoryUuid") final UUID categoryUuid);

    @Query("""
            SELECT DISTINCT w FROM Word w
                       LEFT JOIN FETCH w.wordParts wp
                       LEFT JOIN FETCH w.categories c
                       WHERE EXISTS (SELECT 1 FROM w.categories cat WHERE cat.uuid IN :categoryUuids)
            """)
    List<Word> findByCategoryUuidsWithDetails(@Param("categoryUuids") final Set<UUID> categoryUuids);

    @Query("""
            SELECT w FROM Word w
                        LEFT JOIN FETCH w.wordParts wp
                        LEFT JOIN FETCH w.categories c
                        WHERE w.uuid = :uuid
                        AND EXISTS (SELECT 1 FROM w.categories cat WHERE cat.language.uuid = :languageUuid)
            """)
    Optional<Word> findByUuidAndLanguageUuid(@Param("uuid") final UUID uuid,
                                             @Param("languageUuid") final UUID languageUuid);
}

