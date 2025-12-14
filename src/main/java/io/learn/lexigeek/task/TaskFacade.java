package io.learn.lexigeek.task;

import io.learn.lexigeek.account.dto.AccountDto;
import io.learn.lexigeek.task.dto.TaskDto;
import io.learn.lexigeek.task.dto.TaskScheduleDto;
import io.learn.lexigeek.task.dto.TaskSettingsDto;

import java.util.List;
import java.util.UUID;

public interface TaskFacade {

    List<TaskDto> getTasks();

    List<TaskDto> reloadTasks();

    List<TaskDto> reloadTasks(final AccountDto accountDto);

    void updateTaskProgress(final UUID taskUuid, final Integer current);

    List<TaskSettingsDto> getTaskSettings();

    void updateTaskSettings(final List<TaskSettingsDto> settings);

    TaskScheduleDto getTaskSchedule();

    void updateTaskSchedule(final TaskScheduleDto schedule);
}
