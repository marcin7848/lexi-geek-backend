package io.learn.lexigeek.task.domain;

import io.learn.lexigeek.account.dto.AccountDto;
import io.learn.lexigeek.task.TaskFacade;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;


@Service
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Slf4j
class TaskSchedulerService {

    private final TaskScheduleRepository taskScheduleRepository;
    private final TaskFacade taskFacade;

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void processTaskSchedules() {
        final LocalDateTime now = LocalDateTime.now();
        final List<TaskSchedule> allSchedules = taskScheduleRepository.findAll();
        log.debug("Processing task schedules at {}, found {} schedules", now, allSchedules.size());
        for (final TaskSchedule schedule : allSchedules) {
            if (shouldRunSchedule(schedule, now)) {
                try {
                    reloadTasksForAccount(schedule);
                    schedule.setLastRunAt(now);
                    taskScheduleRepository.save(schedule);
                    log.info("Successfully reloaded tasks for account {} at {}",
                            schedule.getAccount().getUuid(), now);
                } catch (final Exception e) {
                    log.error("Failed to reload tasks for account {}: {}",
                            schedule.getAccount().getUuid(), e.getMessage(), e);
                }
            }
        }
    }

    private boolean shouldRunSchedule(final TaskSchedule schedule, final LocalDateTime now) {
        final LocalTime scheduledTime = LocalTime.of(schedule.getHour(), schedule.getMinute());
        final LocalTime currentTime = now.toLocalTime();

        if (!isWithinMinuteWindow(currentTime, scheduledTime)) {
            return false;
        }
        final LocalDateTime lastRun = schedule.getLastRunAt();

        if (lastRun == null) {
            return shouldRunForFrequency(schedule, now, null);
        }

        if (lastRun.toLocalDate().equals(now.toLocalDate()) &&
                lastRun.getHour() == schedule.getHour() &&
                lastRun.getMinute() == schedule.getMinute()) {
            return false;
        }
        return shouldRunForFrequency(schedule, now, lastRun);
    }

    private boolean isWithinMinuteWindow(final LocalTime currentTime, final LocalTime scheduledTime) {
        return currentTime.getHour() == scheduledTime.getHour() &&
                currentTime.getMinute() == scheduledTime.getMinute();
    }

    private boolean shouldRunForFrequency(final TaskSchedule schedule,
                                          final LocalDateTime now,
                                          final LocalDateTime lastRun) {
        return switch (schedule.getFrequency()) {
            case DAILY -> shouldRunDaily(lastRun, now);
            case EVERY_N_DAYS -> shouldRunEveryNDays(schedule, lastRun, now);
            case WEEKLY -> shouldRunWeekly(schedule, now);
            case MONTHLY -> shouldRunMonthly(schedule, now);
        };
    }

    private boolean shouldRunDaily(final LocalDateTime lastRun, final LocalDateTime now) {
        if (lastRun == null) {
            return true;
        }

        return !lastRun.toLocalDate().equals(now.toLocalDate());
    }

    private boolean shouldRunEveryNDays(final TaskSchedule schedule,
                                        final LocalDateTime lastRun,
                                        final LocalDateTime now) {
        if (lastRun == null) {
            return true;
        }
        final Integer daysInterval = schedule.getFrequencyValue();
        if (daysInterval == null || daysInterval < 1) {
            log.warn("Invalid frequencyValue for EVERY_N_DAYS schedule: {}, defaulting to 1", daysInterval);
            return shouldRunDaily(lastRun, now);
        }

        final LocalDate lastRunDate = lastRun.toLocalDate();
        final LocalDate nextRunDate = lastRunDate.plusDays(daysInterval);
        return !now.toLocalDate().isBefore(nextRunDate);
    }

    private boolean shouldRunWeekly(final TaskSchedule schedule, final LocalDateTime now) {
        final Integer dayOfWeekValue = schedule.getFrequencyValue();
        if (dayOfWeekValue == null || dayOfWeekValue < 1 || dayOfWeekValue > 7) {
            log.warn("Invalid frequencyValue for WEEKLY schedule: {}, defaulting to Monday", dayOfWeekValue);
            return now.getDayOfWeek() == DayOfWeek.MONDAY;
        }

        final DayOfWeek targetDay = DayOfWeek.of(dayOfWeekValue);
        return now.getDayOfWeek() == targetDay;
    }

    private boolean shouldRunMonthly(final TaskSchedule schedule, final LocalDateTime now) {
        final Integer dayOfMonth = schedule.getFrequencyValue();
        if (dayOfMonth == null || dayOfMonth < 1 || dayOfMonth > 31) {
            log.warn("Invalid frequencyValue for MONTHLY schedule: {}, defaulting to day 1", dayOfMonth);
            return now.getDayOfMonth() == 1;
        }

        final int maxDayInMonth = now.toLocalDate().lengthOfMonth();
        final int effectiveDay = Math.min(dayOfMonth, maxDayInMonth);
        return now.getDayOfMonth() == effectiveDay;
    }

    private void reloadTasksForAccount(final TaskSchedule schedule) {
        final Account account = schedule.getAccount();
        final AccountDto accountDto = new AccountDto(
                account.getId(),
                account.getUuid(),
                null,
                null,
                null
        );
        taskFacade.reloadTasks(accountDto);
        log.debug("Generated tasks for account {}", account.getUuid());
    }
}
