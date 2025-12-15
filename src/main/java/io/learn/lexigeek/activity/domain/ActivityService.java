package io.learn.lexigeek.activity.domain;

import io.learn.lexigeek.account.AccountFacade;
import io.learn.lexigeek.account.dto.AccountDto;
import io.learn.lexigeek.activity.ActivityFacade;
import io.learn.lexigeek.activity.dto.ActivityDto;
import io.learn.lexigeek.activity.dto.ActivityFilterForm;
import io.learn.lexigeek.activity.dto.ActivityForm;
import io.learn.lexigeek.common.exception.NotFoundException;
import io.learn.lexigeek.common.pageable.PageDto;
import io.learn.lexigeek.common.pageable.PageableRequest;
import io.learn.lexigeek.common.pageable.PageableUtils;
import io.learn.lexigeek.common.pageable.SortOrder;
import io.learn.lexigeek.common.validation.ErrorCodes;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static io.learn.lexigeek.common.utils.DateTimeUtils.timestampUTC;

@Service
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class ActivityService implements ActivityFacade {

    private final ActivityRepository activityRepository;
    private final AccountRepository accountRepository;
    private final AccountFacade accountFacade;

    @Override
    public PageDto<ActivityDto> getActivities(final ActivityFilterForm form, final PageableRequest pageableRequest) {
        pageableRequest.addDefaultSorts(new SortOrder(Activity.Fields.created, Sort.Direction.DESC));
        final AccountDto account = accountFacade.getLoggedAccount();

        final ActivitySpecification specification = new ActivitySpecification(form, account.id());

        return PageableUtils.toDto(activityRepository.findAll(specification, PageableUtils.createPageable(pageableRequest))
                .map(ActivityMapper::entityToDto), pageableRequest);
    }

    @Override
    @Transactional
    public ActivityDto addActivity(final Long accountId, final ActivityForm form) {
        final Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.USER_NOT_FOUND, accountId));

        final Activity activity = ActivityMapper.formToEntity(form);
        activity.setCreated(timestampUTC());
        activity.setAccount(account);

        final Activity savedActivity = activityRepository.save(activity);

        return ActivityMapper.entityToDto(savedActivity);
    }
}
