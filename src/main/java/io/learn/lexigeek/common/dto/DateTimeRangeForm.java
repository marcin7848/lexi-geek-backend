package io.learn.lexigeek.common.dto;

import io.learn.lexigeek.common.utils.DateTimeUtils;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.Optional;

public record DateTimeRangeForm(@DateTimeFormat(pattern = DateTimeUtils.EXTENDED_DATE_TIME_FORMAT) LocalDateTime min,
                                @DateTimeFormat(pattern = DateTimeUtils.EXTENDED_DATE_TIME_FORMAT) LocalDateTime max)
        implements RangeForm<LocalDateTime> {

    public static DateTimeRangeForm convertToUtc(final DateTimeRangeForm form) {
        if (form == null) {
            return null;
        }

        final LocalDateTime newMin = Optional.ofNullable(form.min)
                .map(DateTimeUtils::toUTCDateTimeFromDefaultDateTime)
                .orElse(null);

        final LocalDateTime newMax = Optional.ofNullable(form.max)
                .map(DateTimeUtils::toUTCDateTimeFromDefaultDateTime)
                .orElse(null);

        return new DateTimeRangeForm(newMin, newMax);
    }
}
