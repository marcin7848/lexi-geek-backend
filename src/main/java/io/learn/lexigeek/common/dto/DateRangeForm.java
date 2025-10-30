package io.learn.lexigeek.common.dto;

import io.learn.lexigeek.common.utils.DateTimeUtils;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record DateRangeForm(@DateTimeFormat(pattern = DateTimeUtils.DEFAULT_LOCAL_DATE_FORMAT) LocalDate min,
                            @DateTimeFormat(pattern = DateTimeUtils.DEFAULT_LOCAL_DATE_FORMAT) LocalDate max)
        implements RangeForm<LocalDate> {

}
