package io.learn.lexigeek.task;

import io.learn.lexigeek.account.dto.AccountDto;
import io.learn.lexigeek.task.dto.TaskDto;
import io.learn.lexigeek.task.dto.TaskScheduleDto;
import io.learn.lexigeek.task.dto.TaskSettingsDto;

import java.util.List;

public interface TaskFacade {

    List<TaskDto> getTasks();

    List<TaskDto> reloadTasks();

    List<TaskDto> reloadTasks(final AccountDto accountDto);

    List<TaskSettingsDto> getTaskSettings();

    void updateTaskSettings(final List<TaskSettingsDto> settings);

    TaskScheduleDto getTaskSchedule();

    void updateTaskSchedule(final TaskScheduleDto schedule);
}
