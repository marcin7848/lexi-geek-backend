package io.learn.lexigeek.activity;

import io.learn.lexigeek.activity.dto.ActivityDto;
import io.learn.lexigeek.activity.dto.ActivityFilterForm;
import io.learn.lexigeek.common.pageable.PageDto;
import io.learn.lexigeek.common.pageable.PageableRequest;

public interface ActivityFacade {

    PageDto<ActivityDto> getActivities(final ActivityFilterForm form, final PageableRequest pageableRequest);
}
