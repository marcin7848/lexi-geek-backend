package io.learn.lexigeek.statistics.domain;

import io.learn.lexigeek.account.AccountFacade;
import io.learn.lexigeek.account.dto.AccountStarsDto;
import io.learn.lexigeek.common.dto.DateRangeForm;
import io.learn.lexigeek.statistics.StatisticsFacade;
import io.learn.lexigeek.statistics.dto.LanguageStats;
import io.learn.lexigeek.statistics.dto.UserStatDto;
import io.learn.lexigeek.word.WordFacade;
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
    public List<UserStatDto> getUserStatistics(final String startDate,
                                               final String endDate,
                                               final List<UUID> languageUuids,
                                               final Boolean showTotal,
                                               final Boolean showStars) {
        final UUID accountUuid = getCurrentAccountUuid();

        final LocalDate start = startDate != null ? LocalDate.parse(startDate, DATE_FORMATTER) : LocalDate.now().minusYears(1);
        final LocalDate end = endDate != null ? LocalDate.parse(endDate, DATE_FORMATTER) : LocalDate.now();

        final List<LocalDate> dateRange = start.datesUntil(end.plusDays(1)).toList();

        final Map<LocalDate, Map<UUID, Integer>> wordStatsData =
                wordFacade.getWordRepeatStatsByDateAndLanguage(accountUuid, start, end, languageUuids);

        final Map<LocalDate, Map<UUID, Integer>> wordCreationData =
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

    private UserStatDto buildUserStatDto(
            final LocalDate date,
            final Map<LocalDate, Map<UUID, Integer>> wordStatsData,
            final Map<LocalDate, Map<UUID, Integer>> wordCreationData,
            final Map<LocalDate, Integer> starsData,
            final Boolean showTotal,
            final Boolean showStars
    ) {
        final Map<UUID, Integer> statsForDate = wordStatsData.getOrDefault(date, new HashMap<>());
        final Map<UUID, Integer> creationForDate = wordCreationData.getOrDefault(date, new HashMap<>());

        // Combine all language UUIDs
        final Set<UUID> allLanguageUuids = new HashSet<>();
        allLanguageUuids.addAll(statsForDate.keySet());
        allLanguageUuids.addAll(creationForDate.keySet());

        // Build language breakdown
        final Map<String, LanguageStats> languageBreakdown = new HashMap<>();
        for (UUID langUuid : allLanguageUuids) {
            final Integer repeatCount = statsForDate.getOrDefault(langUuid, 0);
            final Integer addCount = creationForDate.getOrDefault(langUuid, 0);

            final LanguageStats langStats = new LanguageStats(
                    repeatCount,  // repeatDictionary
                    0,            // repeatExercise (set to 0 as we're aggregating)
                    addCount,     // addDictionary
                    0             // addExercise (set to 0 as we're aggregating)
            );
            languageBreakdown.put(langUuid.toString(), langStats);
        }

        // Calculate totals
        Integer totalRepeatDict = null;
        Integer totalRepeatEx = null;
        Integer totalAddDict = null;
        Integer totalAddEx = null;

        if (showTotal == null || showTotal) {
            totalRepeatDict = statsForDate.values().stream()
                    .mapToInt(Integer::intValue)
                    .sum();
            totalRepeatEx = 0; // Set to 0 as we're aggregating
            totalAddDict = creationForDate.values().stream()
                    .mapToInt(Integer::intValue)
                    .sum();
            totalAddEx = 0; // Set to 0 as we're aggregating
        }

        final Integer stars = (showStars == null || showStars) ? starsData.get(date) : null;

        return new UserStatDto(
                date.format(DATE_FORMATTER),
                totalRepeatDict,
                totalRepeatEx,
                totalAddDict,
                totalAddEx,
                stars,
                languageBreakdown
        );
    }

    private boolean hasData(final UserStatDto dto) {
        return (dto.repeatDictionary() != null && dto.repeatDictionary() > 0) ||
                (dto.repeatExercise() != null && dto.repeatExercise() > 0) ||
                (dto.addDictionary() != null && dto.addDictionary() > 0) ||
                (dto.addExercise() != null && dto.addExercise() > 0) ||
                (dto.stars() != null && dto.stars() > 0);
    }

    private UUID getCurrentAccountUuid() {
        return accountFacade.getLoggedAccount().uuid();
    }
}
