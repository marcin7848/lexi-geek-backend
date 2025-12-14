package io.learn.lexigeek.statistics;

import io.learn.lexigeek.statistics.dto.StatisticsSummary;
import io.learn.lexigeek.statistics.dto.UserStatDto;

import java.util.List;
import java.util.UUID;

public interface StatisticsFacade {

    List<UserStatDto> getUserStatistics(
            String startDate,
            String endDate,
            List<UUID> languageUuids,
            Boolean showTotal,
            Boolean showStars
    );

    List<UserStatDto> getLanguageStatistics(
            UUID languageUuid,
            String startDate,
            String endDate
    );

    StatisticsSummary getStatisticsSummary(
            String startDate,
            String endDate,
            List<UUID> languageUuids,
            Boolean showTotal,
            Boolean showStars
    );
}

