package io.learn.lexigeek.task.controller;

import io.learn.lexigeek.task.TaskFacade;
import io.learn.lexigeek.task.dto.TaskDto;
import io.learn.lexigeek.task.dto.TaskScheduleDto;
import io.learn.lexigeek.task.dto.TaskSettingsDto;
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
        private static final String TASK_SETTINGS = TASKS + "/settings";
        private static final String TASK_SCHEDULE = TASKS + "/schedule";
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

    @GetMapping(Routes.TASK_SETTINGS)
    List<TaskSettingsDto> getTaskSettings() {
        return taskFacade.getTaskSettings();
    }

    @PutMapping(Routes.TASK_SETTINGS)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void updateTaskSettings(@RequestBody @Valid final List<TaskSettingsDto> settings) {
        taskFacade.updateTaskSettings(settings);
    }

    @GetMapping(Routes.TASK_SCHEDULE)
    TaskScheduleDto getTaskSchedule() {
        return taskFacade.getTaskSchedule();
    }

    @PutMapping(Routes.TASK_SCHEDULE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void updateTaskSchedule(@RequestBody @Valid final TaskScheduleDto schedule) {
        taskFacade.updateTaskSchedule(schedule);
    }
}
