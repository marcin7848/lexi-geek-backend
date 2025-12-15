package io.learn.lexigeek.activity.dto;

import io.learn.lexigeek.activity.domain.ActivityType;
import io.learn.lexigeek.common.dto.DateTimeRangeForm;

import java.util.UUID;

public record ActivityFilterForm(UUID languageUuid,
                                 ActivityType type,
                                 DateTimeRangeForm range) {
}
