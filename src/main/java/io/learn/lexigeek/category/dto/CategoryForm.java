package io.learn.lexigeek.category.dto;

import io.learn.lexigeek.category.domain.CategoryMethod;
import io.learn.lexigeek.category.domain.CategoryMode;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CategoryForm(@NotNull CategoryMode mode,
                           @NotNull CategoryMethod method,
                           @NotNull Integer order,
                           UUID parentUuid) {
}

