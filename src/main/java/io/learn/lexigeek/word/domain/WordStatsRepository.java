package io.learn.lexigeek.word.domain;

import io.learn.lexigeek.common.repository.UUIDAwareJpaRepository;
import io.learn.lexigeek.word.dto.WordStatsProjection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
interface WordStatsRepository extends UUIDAwareJpaRepository<WordStats, Long> {

    @Query("""
            SELECT CAST(ws.answerTime AS LocalDate) as date, c.language.uuid as languageUuid, COUNT(ws.id) as count
            FROM WordStats ws
            JOIN ws.word w
            JOIN w.categories c
            WHERE c.language.account.uuid = :accountUuid
                AND CAST(ws.answerTime AS LocalDate) BETWEEN :startDate AND :endDate
                AND (:languageUuids IS NULL OR c.language.uuid IN :languageUuids)
            GROUP BY CAST(ws.answerTime AS LocalDate), c.language.uuid
            """)
    List<WordStatsProjection> findWordRepeatStatsByDateAndLanguage(@Param("accountUuid") final UUID accountUuid,
                                                                   @Param("startDate") final LocalDate startDate,
                                                                   @Param("endDate") final LocalDate endDate,
                                                                   @Param("languageUuids") final List<UUID> languageUuids);
}
