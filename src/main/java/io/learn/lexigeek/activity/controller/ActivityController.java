package io.learn.lexigeek.activity.controller;

import io.learn.lexigeek.activity.ActivityFacade;
import io.learn.lexigeek.activity.dto.ActivityDto;
import io.learn.lexigeek.activity.dto.ActivityFilterForm;
import io.learn.lexigeek.common.pageable.PageDto;
import io.learn.lexigeek.common.pageable.PageableRequest;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@RequestMapping
class ActivityController {

    private static final class Routes {
        private static final String ACTIVITIES = "/activities";
    }

    private final ActivityFacade activityFacade;

    @GetMapping(Routes.ACTIVITIES)
    PageDto<ActivityDto> getActivities(@Valid final ActivityFilterForm form,
                                       @Valid final PageableRequest pageableRequest) {
        return activityFacade.getActivities(form, pageableRequest);
    }
}
