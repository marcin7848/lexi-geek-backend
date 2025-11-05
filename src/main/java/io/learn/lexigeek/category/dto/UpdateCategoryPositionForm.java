package io.learn.lexigeek.category.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateCategoryPositionForm(UUID parentUuid,
                                         @NotNull @Min(value = 0) Integer position) {
}
