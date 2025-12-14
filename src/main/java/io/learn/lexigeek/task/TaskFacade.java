package io.learn.lexigeek.task;

import io.learn.lexigeek.account.dto.AccountDto;
import io.learn.lexigeek.task.dto.TaskConfigDto;
import io.learn.lexigeek.task.dto.TaskDto;
import io.learn.lexigeek.task.dto.TaskType;

import java.util.List;
import java.util.UUID;

public interface TaskFacade {

    List<TaskDto> getTasks();

    List<TaskDto> reloadTasks();

    List<TaskDto> reloadTasks(final AccountDto accountDto);

    TaskConfigDto getTaskConfig();

    void updateTaskConfig(final TaskConfigDto config);

    void fillTask(final TaskType taskType, final UUID languageUuid, final Integer points);
}
