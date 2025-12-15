package io.learn.lexigeek.statistics.domain;

import io.learn.lexigeek.account.AccountFacade;
import io.learn.lexigeek.account.dto.AccountStarsDto;
import io.learn.lexigeek.common.dto.DateRangeForm;
import io.learn.lexigeek.statistics.StatisticsFacade;
import io.learn.lexigeek.statistics.dto.LanguageStats;
import io.learn.lexigeek.statistics.dto.UserStatDto;
import io.learn.lexigeek.word.WordFacade;
import io.learn.lexigeek.word.dto.DateStatItem;
import io.learn.lexigeek.word.dto.LanguageStatItem;
import io.learn.lexigeek.word.dto.WordStatsProjection;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class StatisticsService implements StatisticsFacade {

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

        final List<DateStatItem> wordCreationData =
                wordFacade.getWordCreationStatsByDateAndLanguage(accountUuid, start, end, languageUuids);

        final List<WordStatsProjection> allRepeatStats =
                wordFacade.getAllWordRepeatStatsByDateAndLanguage(accountUuid, start, end, languageUuids);

        final List<DateStatItem> wordStatsData = buildDateStatItemsFiltered(allRepeatStats, true);
        final List<DateStatItem> wordErrorsData = buildDateStatItemsFiltered(allRepeatStats, false);

        final Map<LocalDate, Integer> starsData = new HashMap<>();
        if (showStars == null || showStars) {
            final List<AccountStarsDto> starsList = accountFacade.getStars(new DateRangeForm(start, end));
            starsList.forEach(stars -> starsData.put(stars.date(), stars.stars()));
        }

        return dateRange.stream()
                .map(date -> buildUserStatDto(date, wordStatsData, wordCreationData, wordErrorsData, starsData, showTotal, showStars))
                .toList();
    }

    private UserStatDto buildUserStatDto(final LocalDate date,
                                         final List<DateStatItem> wordStatsData,
                                         final List<DateStatItem> wordCreationData,
                                         final List<DateStatItem> wordErrorsData,
                                         final Map<LocalDate, Integer> starsData,
                                         final Boolean showTotal,
                                         final Boolean showStars) {
        final Map<UUID, Integer> statsForDate = findStatsForDate(wordStatsData, date);
        final Map<UUID, Integer> creationForDate = findStatsForDate(wordCreationData, date);
        final Map<UUID, Integer> errorsForDate = findStatsForDate(wordErrorsData, date);

        final Set<UUID> languageUuids = new HashSet<>();
        languageUuids.addAll(statsForDate.keySet());
        languageUuids.addAll(creationForDate.keySet());
        languageUuids.addAll(errorsForDate.keySet());

        final Map<UUID, LanguageStats> languageStats = new HashMap<>();
        for (final UUID langUuid : languageUuids) {
            final Integer repeatCount = statsForDate.getOrDefault(langUuid, 0);
            final Integer addCount = creationForDate.getOrDefault(langUuid, 0);
            final Integer errorsCount = errorsForDate.getOrDefault(langUuid, 0);

            final LanguageStats langStats = new LanguageStats(
                    repeatCount,
                    addCount,
                    errorsCount
            );
            languageStats.put(langUuid, langStats);
        }

        Integer totalRepeat = null;
        Integer totalAdd = null;
        Integer totalErrors = null;

        if (showTotal == null || showTotal) {
            totalRepeat = statsForDate.values().stream()
                    .mapToInt(Integer::intValue)
                    .sum();
            totalAdd = creationForDate.values().stream()
                    .mapToInt(Integer::intValue)
                    .sum();
            totalErrors = errorsForDate.values().stream()
                    .mapToInt(Integer::intValue)
                    .sum();
        }

        final Integer stars = (showStars == null || showStars) ? starsData.get(date) : null;

        return new UserStatDto(
                date,
                totalRepeat,
                totalAdd,
                stars,
                totalErrors,
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

    private List<DateStatItem> buildDateStatItemsFiltered(final List<WordStatsProjection> projections,
                                                          final boolean correctFilter) {
        final Map<LocalDate, List<LanguageStatItem>> groupedByDate = new HashMap<>();

        for (final WordStatsProjection projection : projections) {
            if (projection.getCorrect() != null && projection.getCorrect() == correctFilter) {
                final LocalDate date = projection.getDate();
                final LanguageStatItem item = new LanguageStatItem(projection.getLanguageUuid(), projection.getCount());
                groupedByDate.computeIfAbsent(date, d -> new ArrayList<>()).add(item);
            }
        }

        return groupedByDate.entrySet().stream()
                .map(entry -> new DateStatItem(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(DateStatItem::date))
                .toList();
    }

    private UUID getCurrentAccountUuid() {
        return accountFacade.getLoggedAccount().uuid();
    }
}
