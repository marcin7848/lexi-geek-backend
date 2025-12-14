package io.learn.lexigeek.task.controller;

import io.learn.lexigeek.task.TaskFacade;
import io.learn.lexigeek.task.dto.TaskConfigDto;
import io.learn.lexigeek.task.dto.TaskDto;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@RequestMapping
class TaskController {

    private static final class Routes {
        private static final String TASKS = "/tasks";
        private static final String TASKS_RELOAD = TASKS + "/reload";
        private static final String TASK_CONFIG = TASKS + "/config";
    }

    private final TaskFacade taskFacade;

    @GetMapping(Routes.TASKS)
    List<TaskDto> getTasks() {
        return taskFacade.getTasks();
    }

    @PostMapping(Routes.TASKS_RELOAD)
    @ResponseStatus(HttpStatus.OK)
    List<TaskDto> reloadTasks() {
        return taskFacade.reloadTasks();
    }

    @GetMapping(Routes.TASK_CONFIG)
    TaskConfigDto getTaskConfig() {
        return taskFacade.getTaskConfig();
    }

    @PutMapping(Routes.TASK_CONFIG)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void updateTaskConfig(@RequestBody @Valid final TaskConfigDto config) {
        taskFacade.updateTaskConfig(config);
    }
}
