package io.learn.lexigeek.statistics.domain;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Repository
class WordQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Get word creation stats grouped by date and language.
     * Stats are counted from words table based on created timestamp.
     */
    public Map<LocalDate, Map<UUID, LanguageStatsData>> getWordCreationByDateAndLanguages(
            final UUID accountUuid,
            final LocalDate startDate,
            final LocalDate endDate,
            final List<UUID> languageUuids
    ) {
        StringBuilder queryStr = new StringBuilder("""
            SELECT 
                CAST(w.created AS DATE) as creation_date,
                c.language_id as language_uuid,
                c.mode as category_mode,
                COUNT(DISTINCT w.id) as count
            FROM words w
            JOIN category_word cw ON w.id = cw.word_id
            JOIN categories c ON cw.category_id = c.id
            JOIN languages l ON c.language_id = l.id
            WHERE l.account_id = (SELECT id FROM accounts WHERE uuid = :accountUuid)
                AND CAST(w.created AS DATE) BETWEEN :startDate AND :endDate
            """);

        if (languageUuids != null && !languageUuids.isEmpty()) {
            queryStr.append(" AND c.language_id IN (SELECT id FROM languages WHERE uuid IN :languageUuids)");
        }

        queryStr.append(" GROUP BY CAST(w.created AS DATE), c.language_id, c.mode");

        Query query = entityManager.createNativeQuery(queryStr.toString());
        query.setParameter("accountUuid", accountUuid.toString());
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        if (languageUuids != null && !languageUuids.isEmpty()) {
            List<String> uuidStrings = languageUuids.stream()
                    .map(UUID::toString)
                    .collect(Collectors.toList());
            query.setParameter("languageUuids", uuidStrings);
        }

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        Map<LocalDate, Map<UUID, LanguageStatsData>> resultMap = new HashMap<>();

        for (Object[] row : results) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            UUID languageUuid = UUID.fromString(row[1].toString());
            String categoryMode = (String) row[2];
            int count = ((Number) row[3]).intValue();

            resultMap.computeIfAbsent(date, k -> new HashMap<>());
            Map<UUID, LanguageStatsData> langMap = resultMap.get(date);

            LanguageStatsData existing = langMap.getOrDefault(languageUuid,
                    new LanguageStatsData(0, 0, 0, 0));

            LanguageStatsData updated;
            if ("DICTIONARY".equals(categoryMode)) {
                updated = new LanguageStatsData(
                        existing.repeatDictionary(),
                        existing.repeatExercise(),
                        existing.addDictionary() + count,
                        existing.addExercise()
                );
            } else { // EXERCISE
                updated = new LanguageStatsData(
                        existing.repeatDictionary(),
                        existing.repeatExercise(),
                        existing.addDictionary(),
                        existing.addExercise() + count
                );
            }

            langMap.put(languageUuid, updated);
        }

        return resultMap;
    }
}

