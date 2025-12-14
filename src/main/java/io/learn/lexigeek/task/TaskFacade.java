package io.learn.lexigeek.task;

import io.learn.lexigeek.task.dto.TaskDto;
import io.learn.lexigeek.task.dto.TaskScheduleDto;
import io.learn.lexigeek.task.dto.TaskSettingsDto;

import java.util.List;
import java.util.UUID;

public interface TaskFacade {

    List<TaskDto> getTasks();

    List<TaskDto> reloadTasks();

    void updateTaskProgress(UUID taskUuid, Integer current);

    List<TaskSettingsDto> getTaskSettings();

    void updateTaskSettings(List<TaskSettingsDto> settings);

    TaskScheduleDto getTaskSchedule();

    void updateTaskSchedule(TaskScheduleDto schedule);
}
