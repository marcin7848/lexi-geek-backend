package io.learn.lexigeek.statistics.domain;

import io.learn.lexigeek.account.AccountFacade;
import io.learn.lexigeek.statistics.StatisticsFacade;
import io.learn.lexigeek.statistics.dto.*;
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

    private final WordStatsQueryRepository wordStatsQueryRepository;
    private final WordQueryRepository wordQueryRepository;
    private final AccountFacade accountFacade;

    @Override
    @Transactional(readOnly = true)
    public List<UserStatDto> getUserStatistics(
            final String startDate,
            final String endDate,
            final List<UUID> languageUuids,
            final Boolean showTotal,
            final Boolean showStars
    ) {
        final UUID accountUuid = getCurrentAccountUuid();

        final LocalDate start = startDate != null ? LocalDate.parse(startDate, DATE_FORMATTER) : LocalDate.now().minusYears(1);
        final LocalDate end = endDate != null ? LocalDate.parse(endDate, DATE_FORMATTER) : LocalDate.now();

        // Get all dates in range
        final List<LocalDate> dateRange = start.datesUntil(end.plusDays(1)).toList();

        // Get word stats data (repeated)
        final Map<LocalDate, Map<UUID, LanguageStatsData>> wordStatsData =
                wordStatsQueryRepository.getWordStatsByDateAndLanguages(accountUuid, start, end, languageUuids);

        // Get word creation data (added)
        final Map<LocalDate, Map<UUID, LanguageStatsData>> wordCreationData =
                wordQueryRepository.getWordCreationByDateAndLanguages(accountUuid, start, end, languageUuids);

        // Get stars data if needed
        final Map<LocalDate, Integer> starsData = new HashMap<>();
        if (showStars == null || showStars) {
            final var starsList = languageUuids != null && !languageUuids.isEmpty()
                    ? accountStarsRepository.findByAccountUuidAndDateBetween(accountUuid, start, end)
                    : accountStarsRepository.findByAccountUuidAndDateBetween(accountUuid, start, end);

            starsList.forEach(stars -> starsData.put(stars.getCreated(), stars.getStars()));
        }

        // Build result for each date
        return dateRange.stream()
                .map(date -> buildUserStatDto(date, wordStatsData, wordCreationData, starsData, showTotal, showStars))
                .filter(dto -> hasData(dto))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserStatDto> getLanguageStatistics(
            final UUID languageUuid,
            final String startDate,
            final String endDate
    ) {
        return getUserStatistics(startDate, endDate, List.of(languageUuid), true, true);
    }

    @Override
    @Transactional(readOnly = true)
    public StatisticsSummary getStatisticsSummary(
            final String startDate,
            final String endDate,
            final List<UUID> languageUuids,
            final Boolean showTotal,
            final Boolean showStars
    ) {
        final List<UserStatDto> stats = getUserStatistics(startDate, endDate, languageUuids, showTotal, showStars);

        final int totalRepeatedWords = stats.stream()
                .mapToInt(dto -> (dto.repeatDictionary() != null ? dto.repeatDictionary() : 0) +
                                (dto.repeatExercise() != null ? dto.repeatExercise() : 0))
                .sum();

        final int totalAddedWords = stats.stream()
                .mapToInt(dto -> (dto.addDictionary() != null ? dto.addDictionary() : 0) +
                                (dto.addExercise() != null ? dto.addExercise() : 0))
                .sum();

        final int totalStars = stats.stream()
                .mapToInt(dto -> dto.stars() != null ? dto.stars() : 0)
                .sum();

        final long dayCount = stats.size();
        final double avgRepeated = dayCount > 0 ? (double) totalRepeatedWords / dayCount : 0.0;
        final double avgAdded = dayCount > 0 ? (double) totalAddedWords / dayCount : 0.0;
        final double avgStars = dayCount > 0 ? (double) totalStars / dayCount : 0.0;

        return new StatisticsSummary(
                totalRepeatedWords,
                totalAddedWords,
                totalStars,
                new AveragePerDay(avgRepeated, avgAdded, avgStars)
        );
    }

    private UserStatDto buildUserStatDto(
            final LocalDate date,
            final Map<LocalDate, Map<UUID, LanguageStatsData>> wordStatsData,
            final Map<LocalDate, Map<UUID, LanguageStatsData>> wordCreationData,
            final Map<LocalDate, Integer> starsData,
            final Boolean showTotal,
            final Boolean showStars
    ) {
        final Map<UUID, LanguageStatsData> statsForDate = wordStatsData.getOrDefault(date, new HashMap<>());
        final Map<UUID, LanguageStatsData> creationForDate = wordCreationData.getOrDefault(date, new HashMap<>());

        // Combine all language UUIDs
        final Set<UUID> allLanguageUuids = new HashSet<>();
        allLanguageUuids.addAll(statsForDate.keySet());
        allLanguageUuids.addAll(creationForDate.keySet());

        // Build language breakdown
        final Map<String, LanguageStats> languageBreakdown = new HashMap<>();
        for (UUID langUuid : allLanguageUuids) {
            final LanguageStatsData stats = statsForDate.get(langUuid);
            final LanguageStatsData creation = creationForDate.get(langUuid);

            final LanguageStats langStats = new LanguageStats(
                    stats != null ? stats.repeatDictionary() : 0,
                    stats != null ? stats.repeatExercise() : 0,
                    creation != null ? creation.addDictionary() : 0,
                    creation != null ? creation.addExercise() : 0
            );
            languageBreakdown.put(langUuid.toString(), langStats);
        }

        // Calculate totals
        Integer totalRepeatDict = null;
        Integer totalRepeatEx = null;
        Integer totalAddDict = null;
        Integer totalAddEx = null;

        if (showTotal == null || showTotal) {
            totalRepeatDict = languageBreakdown.values().stream()
                    .mapToInt(LanguageStats::repeatDictionary)
                    .sum();
            totalRepeatEx = languageBreakdown.values().stream()
                    .mapToInt(LanguageStats::repeatExercise)
                    .sum();
            totalAddDict = languageBreakdown.values().stream()
                    .mapToInt(LanguageStats::addDictionary)
                    .sum();
            totalAddEx = languageBreakdown.values().stream()
                    .mapToInt(LanguageStats::addExercise)
                    .sum();
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

