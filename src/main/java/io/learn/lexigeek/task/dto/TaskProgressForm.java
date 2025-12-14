package io.learn.lexigeek.task.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

public record TaskProgressForm(@NotNull @Min(0) Integer current) {
}
