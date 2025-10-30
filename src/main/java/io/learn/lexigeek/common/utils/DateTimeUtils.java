package io.learn.lexigeek.common.utils;

import io.learn.lexigeek.common.entity.DateRange;
import io.learn.lexigeek.common.dto.DateRangeForm;
import org.apache.commons.lang3.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.time.ZoneOffset.UTC;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toSet;

public class DateTimeUtils {

    public static final String DEFAULT_LOCAL_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm";
    public static final String EXTENDED_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String DEFAULT_LOCAL_DATE_FORMAT = "yyyy-MM-dd";
    public static final String OFFSET_DATE_TIME_DEFAULT_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    public static final ZoneId DEFAULT_TIME_ZONE_ID = ZoneId.of("Europe/Warsaw");
    public static final String DEFAULT_LOCAL_TIME_FORMAT = "HH:mm";

    public static LocalDateTime timestampUTC() {
        return LocalDateTime.now(Clock.systemUTC());
    }

    public static LocalDate actualDateUTC() {
        return timestampUTC().toLocalDate();
    }

    public static LocalTime actualTimeUTC() {
        return timestampUTC().toLocalTime();
    }

    public static LocalDateTime timestampAtDefaultZone() {
        return now().atZone(DEFAULT_TIME_ZONE_ID).toLocalDateTime();
    }

    public static LocalTime actualTimeAtDefaultZone(final Clock clock) {
        return now(clock).atZone(DEFAULT_TIME_ZONE_ID).toLocalTime();
    }

    public static Instant now() {
        return Instant.now(Clock.systemUTC());
    }

    public static Instant now(final Clock clock) {
        return Instant.now(clock);
    }

    public static LocalDate actualDateAtDefaultTimeZone() {
        return LocalDate.now(DEFAULT_TIME_ZONE_ID);
    }

    public static LocalDateTime toLocalDateTime(final Instant instant) {
        return instant.atZone(DEFAULT_TIME_ZONE_ID)
                .toLocalDateTime();
    }

    public static LocalDateTime toUTCDateTimeFromDefaultDateTime(final LocalDateTime dateTime) {
        return dateTime.atZone(DEFAULT_TIME_ZONE_ID)
                .withZoneSameInstant(UTC)
                .toLocalDateTime();
    }

    public static LocalDateTime toUTCLocalDateTime(final Instant instant) {
        return LocalDateTime.ofInstant(instant, UTC);
    }

    public static String toFormattedLocalDateTime(final Instant instant) {
        return toLocalDateTime(instant)
                .format(DateTimeFormatter.ofPattern(DEFAULT_LOCAL_DATE_TIME_FORMAT));
    }

    public static String toFormattedUtcDateTime(final Instant instant) {
        return LocalDateTime.ofInstant(instant, UTC)
                .format(DateTimeFormatter.ofPattern(EXTENDED_DATE_TIME_FORMAT));
    }

    public static String toDefaultLocalDate(final LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern(DEFAULT_LOCAL_DATE_FORMAT));
    }

    public static String isoFormattedTimestamp() {
        return DateTimeFormatter.ofPattern(OFFSET_DATE_TIME_DEFAULT_FORMAT).withZone(UTC).format(now());
    }

    public static LocalDate fromStringToLocalDate(final String date) {
        return LocalDate.parse(date, DateTimeFormatter.ofPattern(DEFAULT_LOCAL_DATE_FORMAT));
    }

    public static String toFormattedDefaultZoneLocalDateTime(final LocalDateTime dateTime) {
        return dateTime.atZone(UTC).withZoneSameInstant(DEFAULT_TIME_ZONE_ID).format(DateTimeFormatter.ofPattern(DEFAULT_LOCAL_DATE_TIME_FORMAT));
    }

    public static Date fromLocalDateAtUTCToDate(final LocalDate localDate) {
        return Date.from(localDate.atStartOfDay().toInstant(UTC));
    }

    public static LocalDate fromDateToLocalDateAtUTC(final Date date) {
        return date.toInstant().atOffset(UTC).toLocalDate();
    }

    public static Date fromLocalDateTimeAtUTCToDate(final LocalDateTime localDateTime) {
        return Date.from(localDateTime.toInstant(UTC));
    }

    public static Instant fromLocalDateAtUTCToInstant(final LocalDate localDate) {
        return localDate.atStartOfDay().toInstant(UTC);
    }

    public static LocalDateTime fromDateToLocalDateTimeAtUTC(final Date date) {
        return date.toInstant().atOffset(UTC).toLocalDateTime();
    }

    public static Date fromOffsetDateTimeToDate(final OffsetDateTime offsetDateTime) {
        return Date.from(offsetDateTime.toInstant());
    }

    public static OffsetDateTime fromDateToOffsetDateTimeAtUTC(final Date date) {
        return date.toInstant().atOffset(UTC);
    }

    public static LocalDate actualDateByTimeZoneId(final String timeZoneId) {
        return now().atZone(ZoneId.of(timeZoneId)).toLocalDate();
    }

    public static LocalTime actualTimeByTimeZoneId(final String timeZoneId) {
        return now().atZone(ZoneId.of(timeZoneId)).toLocalTime();
    }

    public static TimeZone getDefaultTimeZone() {
        return TimeZone.getTimeZone(DEFAULT_TIME_ZONE_ID);
    }

    public static Set<LocalDate> fromRangeToDatesSet(final String dateRange) {
        if (StringUtils.isBlank(dateRange)) {
            return Set.of();
        }
        return Stream.of(dateRange.split(","))
                .flatMap(range -> {
                    final String[] dates = range.contains("%7C") ? range.split("%7C") : range.split("\\|");
                    final LocalDate min = LocalDate.parse(dates[0], DateTimeFormatter.ofPattern(DEFAULT_LOCAL_DATE_FORMAT));
                    if (dates.length > 1) {
                        final LocalDate max = LocalDate.parse(dates[1], DateTimeFormatter.ofPattern(DEFAULT_LOCAL_DATE_FORMAT));
                        return min.datesUntil(max.plusDays(1));
                    } else {
                        return Stream.of(min);
                    }
                })
                .collect(toSet());
    }

    public static Set<DateRange> fromRangeStringToDateRange(final String dateRange) {
        if (isNull(dateRange) || dateRange.isEmpty()) {
            return new HashSet<>();
        }
        return Stream.of(dateRange.split(","))
                .flatMap(dateRangeStr -> Stream.ofNullable(parseDateRange(dateRangeStr)))
                .collect(toSet());
    }

    private static DateRange parseDateRange(final String dateRange) {
        final String[] parts = dateRange.split("\\|");
        final LocalDate min = LocalDate.parse(parts[0]);
        final LocalDate max = parts.length > 1 ? LocalDate.parse(parts[1]) : null;
        return new DateRange(min, max);
    }

    public static String toDateRangeString(final DateRange dateRange) {
        if (dateRange == null || dateRange.min() == null) {
            return "";
        }

        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DEFAULT_LOCAL_DATE_FORMAT);
        if (dateRange.max() == null) {
            return dateRange.min().format(formatter);
        }

        return dateRange.min().format(formatter) + "|" + dateRange.max().format(formatter);
    }

    public static String formatDatesAsRanges(final Set<LocalDate> dates) {
        final List<LocalDate> sortedDates = dates.stream().sorted().toList();
        final List<String> ranges = new ArrayList<>();
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DEFAULT_LOCAL_DATE_FORMAT);

        IntStream.range(0, sortedDates.size()).boxed().forEach(i -> {
            if (i == 0 || !sortedDates.get(i).minusDays(1).equals(sortedDates.get(i - 1))) {
                final LocalDate min = sortedDates.get(i);
                LocalDate max = min;
                while (i + 1 < sortedDates.size() && sortedDates.get(i + 1).equals(max.plusDays(1))) {
                    max = sortedDates.get(i + 1);
                    i++;
                }
                if (min.equals(max)) {
                    ranges.add(min.format(formatter));
                } else {
                    ranges.add(min.format(formatter) + "|" + max.format(formatter));
                }
            }
        });

        return String.join(",", ranges);
    }

    public static DateRange mapDateRangeFormToDateRange(final DateRangeForm dateRangeForm) {
        return new DateRange(
                dateRangeForm.min(),
                dateRangeForm.max()
        );
    }

    public static Set<DateRange> mapDateRangesFormToDateRanges(final Set<DateRangeForm> dateRangeForm) {
        return dateRangeForm.stream()
                .map(DateTimeUtils::mapDateRangeFormToDateRange)
                .collect(toSet());
    }
}
