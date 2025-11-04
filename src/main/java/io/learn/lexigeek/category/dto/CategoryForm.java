package io.learn.lexigeek.category.dto;

import io.learn.lexigeek.category.domain.CategoryMethod;
import io.learn.lexigeek.category.domain.CategoryMode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CategoryForm(@NotNull @Size(min = 3, max = 255) String name,
                           @NotNull CategoryMode mode,
                           @NotNull CategoryMethod method,
                           @NotNull Integer position,
                           UUID parentUuid) {
}

