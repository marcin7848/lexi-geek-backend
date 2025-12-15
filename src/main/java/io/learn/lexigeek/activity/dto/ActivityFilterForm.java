package io.learn.lexigeek.activity.dto;

import io.learn.lexigeek.activity.domain.ActivityType;
import io.learn.lexigeek.common.dto.DateTimeRangeForm;

import java.util.UUID;

public record ActivityFilterForm(UUID languageUuid,
                                 UUID categoryUuid,
                                 ActivityType type,
                                 DateTimeRangeForm range) {
}
