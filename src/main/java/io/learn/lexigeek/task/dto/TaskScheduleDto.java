package io.learn.lexigeek.task.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record TaskScheduleDto(@NotNull @Min(0) @Max(23) Integer hour,
                              @NotNull @Min(0) @Max(59) Integer minute,
                              @NotNull TaskFrequency frequency,
                              Integer frequencyValue,
                              LocalDateTime lastRunAt) {
}
