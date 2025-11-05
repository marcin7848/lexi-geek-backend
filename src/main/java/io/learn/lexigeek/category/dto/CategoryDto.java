package io.learn.lexigeek.category.dto;

import io.learn.lexigeek.category.domain.CategoryMethod;
import io.learn.lexigeek.category.domain.CategoryMode;

import java.util.UUID;

public record CategoryDto(UUID uuid,
                          UUID parentUuid,
                          String name,
                          CategoryMode mode,
                          CategoryMethod method,
                          Integer position) {
}

