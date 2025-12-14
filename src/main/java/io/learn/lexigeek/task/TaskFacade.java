package io.learn.lexigeek.task;

import io.learn.lexigeek.account.dto.AccountDto;
import io.learn.lexigeek.task.dto.TaskConfigDto;
import io.learn.lexigeek.task.dto.TaskDto;
import io.learn.lexigeek.task.dto.TaskType;

import java.util.List;

public interface TaskFacade {

    List<TaskDto> getTasks();

    List<TaskDto> reloadTasks();

    List<TaskDto> reloadTasks(final AccountDto accountDto);

    TaskConfigDto getTaskConfig();

    void updateTaskConfig(final TaskConfigDto config);

    void fillTask(final TaskType taskType, final Integer points);
}
