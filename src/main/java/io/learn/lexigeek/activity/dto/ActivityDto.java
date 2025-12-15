package io.learn.lexigeek.activity.dto;

import java.util.UUID;

public record ActivityDto(UUID uuid,
                          String languageName,
                          String title,
                          String created,
                          String type,
                          String param) {
}
