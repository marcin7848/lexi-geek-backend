package io.learn.lexigeek.activity.dto;

import io.learn.lexigeek.activity.domain.ActivityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ActivityForm(@NotNull ActivityType type,
                           @NotBlank String languageName,
                           String title,
                           String param) {
}
