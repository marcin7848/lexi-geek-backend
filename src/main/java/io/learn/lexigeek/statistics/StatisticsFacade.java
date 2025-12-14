package io.learn.lexigeek.statistics;

import io.learn.lexigeek.statistics.dto.UserStatDto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface StatisticsFacade {

    List<UserStatDto> getUserStatistics(final LocalDate startDate, final LocalDate endDate,
                                        final List<UUID> languageUuids, final Boolean showTotal, final Boolean showStars);
}
