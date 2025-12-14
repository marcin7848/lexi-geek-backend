package io.learn.lexigeek.word.domain;

import io.learn.lexigeek.common.repository.UUIDAwareJpaRepository;
import io.learn.lexigeek.word.dto.WordStatsProjection;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    List<Word> findByCategoryUuids(@Param("categoryUuids") final Set<UUID> categoryUuids);

    @Query("""
            SELECT w FROM Word w
                        LEFT JOIN FETCH w.wordParts wp
                        LEFT JOIN FETCH w.categories c
                        WHERE w.uuid = :uuid
                        AND EXISTS (SELECT 1 FROM w.categories cat WHERE cat.language.uuid = :languageUuid)
            """)
    Optional<Word> findByUuidAndLanguageUuid(@Param("uuid") final UUID uuid,
                                             @Param("languageUuid") final UUID languageUuid);

    @Modifying
    @Query("""
            UPDATE Word w SET w.resetTime = :resetTime
            WHERE EXISTS (SELECT 1 FROM w.categories cat WHERE cat.uuid = :categoryUuid)
            """)
    void updateResetTimeByCategoryUuid(@Param("categoryUuid") final UUID categoryUuid,
                                       @Param("resetTime") final LocalDateTime resetTime);

    @Modifying
    @Query("""
            UPDATE Word w SET w.resetTime = :resetTime
            WHERE EXISTS (SELECT 1 FROM w.categories cat WHERE cat.language.uuid = :languageUuid)
            """)
    void updateResetTimeByLanguageUuid(@Param("languageUuid") final UUID languageUuid,
                                       @Param("resetTime") final LocalDateTime resetTime);

    @Query("""
            SELECT CAST(w.created AS LocalDate) as date, c.language.uuid as languageUuid, COUNT(DISTINCT w.id) as count
            FROM Word w
            JOIN w.categories c
            WHERE c.language.account.uuid = :accountUuid
                AND CAST(w.created AS LocalDate) BETWEEN :startDate AND :endDate
                AND (:languageUuids IS NULL OR c.language.uuid IN :languageUuids)
            GROUP BY CAST(w.created AS LocalDate), c.language.uuid
            """)
    List<WordStatsProjection> findWordCreationStatsByDateAndLanguage(@Param("accountUuid") final UUID accountUuid,
                                                                     @Param("startDate") final LocalDate startDate,
                                                                     @Param("endDate") final LocalDate endDate,
                                                                     @Param("languageUuids") final List<UUID> languageUuids);
}
