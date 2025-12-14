package io.learn.lexigeek.statistics.domain;

import io.learn.lexigeek.account.AccountFacade;
import io.learn.lexigeek.account.dto.AccountStarsDto;
import io.learn.lexigeek.common.dto.DateRangeForm;
import io.learn.lexigeek.statistics.StatisticsFacade;
import io.learn.lexigeek.statistics.dto.LanguageStats;
import io.learn.lexigeek.statistics.dto.UserStatDto;
import io.learn.lexigeek.word.WordFacade;
import io.learn.lexigeek.word.dto.DateStatItem;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class StatisticsService implements StatisticsFacade {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE;

    private final WordFacade wordFacade;
    private final AccountFacade accountFacade;

    @Override
    @Transactional(readOnly = true)
    public List<UserStatDto> getUserStatistics(final LocalDate startDate,
                                               final LocalDate endDate,
                                               final List<UUID> languageUuids,
                                               final Boolean showTotal,
                                               final Boolean showStars) {
        final UUID accountUuid = getCurrentAccountUuid();

        final LocalDate start = startDate != null ? startDate : LocalDate.now().minusYears(1);
        final LocalDate end = endDate != null ? endDate : LocalDate.now();

        final List<LocalDate> dateRange = start.datesUntil(end.plusDays(1)).toList();

        final List<DateStatItem> wordStatsData =
                wordFacade.getWordRepeatStatsByDateAndLanguage(accountUuid, start, end, languageUuids);

        final List<DateStatItem> wordCreationData =
                wordFacade.getWordCreationStatsByDateAndLanguage(accountUuid, start, end, languageUuids);

        final Map<LocalDate, Integer> starsData = new HashMap<>();
        if (showStars == null || showStars) {
            final List<AccountStarsDto> starsList = accountFacade.getStars(new DateRangeForm(start, end));
            starsList.forEach(stars -> starsData.put(stars.date(), stars.stars()));
        }

        return dateRange.stream()
                .map(date -> buildUserStatDto(date, wordStatsData, wordCreationData, starsData, showTotal, showStars))
                .filter(this::hasData)
                .collect(Collectors.toList());
    }


    private UserStatDto buildUserStatDto(final LocalDate date,
                                         final List<DateStatItem> wordStatsData,
                                         final List<DateStatItem> wordCreationData,
                                         final Map<LocalDate, Integer> starsData,
                                         final Boolean showTotal,
                                         final Boolean showStars) {
        final Map<UUID, Integer> statsForDate = findStatsForDate(wordStatsData, date);
        final Map<UUID, Integer> creationForDate = findStatsForDate(wordCreationData, date);

        final Set<UUID> languageUuids = new HashSet<>();
        languageUuids.addAll(statsForDate.keySet());
        languageUuids.addAll(creationForDate.keySet());

        final Map<UUID, LanguageStats> languageStats = new HashMap<>();
        for (final UUID langUuid : languageUuids) {
            final Integer repeatCount = statsForDate.getOrDefault(langUuid, 0);
            final Integer addCount = creationForDate.getOrDefault(langUuid, 0);

            final LanguageStats langStats = new LanguageStats(
                    repeatCount,
                    addCount
            );
            languageStats.put(langUuid, langStats);
        }

        Integer totalRepeat = null;
        Integer totalAdd = null;

        if (showTotal == null || showTotal) {
            totalRepeat = statsForDate.values().stream()
                    .mapToInt(Integer::intValue)
                    .sum();
            totalAdd = creationForDate.values().stream()
                    .mapToInt(Integer::intValue)
                    .sum();
        }

        final Integer stars = (showStars == null || showStars) ? starsData.get(date) : null;

        return new UserStatDto(
                date,
                totalRepeat,
                totalAdd,
                stars,
                languageStats
        );
    }

    private Map<UUID, Integer> findStatsForDate(final List<DateStatItem> dateStatItems, final LocalDate date) {
        return dateStatItems.stream()
                .filter(item -> item.date().equals(date))
                .findFirst()
                .map(item -> {
                    final Map<UUID, Integer> result = new HashMap<>();
                    item.languageStats().forEach(langStat ->
                            result.put(langStat.languageUuid(), langStat.count()));
                    return result;
                })
                .orElse(new HashMap<>());
    }

    private boolean hasData(final UserStatDto dto) {
        return (dto.repeat() != null && dto.repeat() > 0) ||
                (dto.add() != null && dto.add() > 0) ||
                (dto.stars() != null && dto.stars() > 0);
    }

    private UUID getCurrentAccountUuid() {
        return accountFacade.getLoggedAccount().uuid();
    }
}
