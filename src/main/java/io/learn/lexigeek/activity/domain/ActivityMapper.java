package io.learn.lexigeek.activity.domain;

import io.learn.lexigeek.activity.dto.ActivityDto;
import lombok.experimental.UtilityClass;

import static io.learn.lexigeek.common.utils.DateTimeUtils.ISO_FORMATTER;

@UtilityClass
class ActivityMapper {

    ActivityDto entityToDto(final Activity activity) {
        return new ActivityDto(
                activity.getUuid(),
                activity.getLanguageName(),
                activity.getCategoryName(),
                ISO_FORMATTER.format(activity.getCreated()),
                activity.getType().name());
    }
}
