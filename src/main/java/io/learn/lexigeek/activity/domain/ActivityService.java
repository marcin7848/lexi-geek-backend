package io.learn.lexigeek.activity.domain;

import io.learn.lexigeek.account.AccountFacade;
import io.learn.lexigeek.account.dto.AccountDto;
import io.learn.lexigeek.activity.ActivityFacade;
import io.learn.lexigeek.activity.dto.ActivityDto;
import io.learn.lexigeek.activity.dto.ActivityFilterForm;
import io.learn.lexigeek.common.pageable.PageDto;
import io.learn.lexigeek.common.pageable.PageableRequest;
import io.learn.lexigeek.common.pageable.PageableUtils;
import io.learn.lexigeek.common.pageable.SortOrder;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class ActivityService implements ActivityFacade {

    private final ActivityRepository activityRepository;
    private final AccountFacade accountFacade;

    @Override
    public PageDto<ActivityDto> getActivities(final ActivityFilterForm form, final PageableRequest pageableRequest) {
        pageableRequest.addDefaultSorts(new SortOrder(Activity.Fields.created, Sort.Direction.DESC));
        final AccountDto account = accountFacade.getLoggedAccount();

        final ActivitySpecification specification = new ActivitySpecification(form, account.id());

        return PageableUtils.toDto(activityRepository.findAll(specification, PageableUtils.createPageable(pageableRequest))
                .map(ActivityMapper::entityToDto), pageableRequest);
    }
}
