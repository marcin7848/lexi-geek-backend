package io.learn.lexigeek.word.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record StartRepeatSessionForm(@NotEmpty List<UUID> categoryUuids,
                                     @NotNull
                                     @Min(value = 1) Integer wordCount,
                                     @NotNull RepeatMethod method) {
}
