package io.learn.lexigeek.task.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record TaskConfigDto(@NotNull @Valid List<TaskSettingsDto> settings,
                            @NotNull @Valid TaskScheduleDto schedule) {
}
