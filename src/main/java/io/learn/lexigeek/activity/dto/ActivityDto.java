package io.learn.lexigeek.activity.dto;

import java.util.UUID;

public record ActivityDto(UUID uuid,
                          String languageName,
                          String categoryName,
                          String created,
                          String type,
                          String param) {
}
