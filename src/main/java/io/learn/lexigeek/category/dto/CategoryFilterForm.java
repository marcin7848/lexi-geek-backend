package io.learn.lexigeek.category.dto;

import io.learn.lexigeek.category.domain.CategoryMethod;
import io.learn.lexigeek.category.domain.CategoryMode;

import java.util.UUID;

public record CategoryFilterForm(UUID uuid,
                                 UUID parentUuid,
                                 CategoryMode mode,
                                 CategoryMethod method,
                                 Integer order) {
}

