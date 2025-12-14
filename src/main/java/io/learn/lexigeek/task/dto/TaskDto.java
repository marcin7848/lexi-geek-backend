package io.learn.lexigeek.task.dto;

import java.util.UUID;

public record TaskDto(UUID uuid,
                      TaskType type,
                      UUID languageUuid,
                      String languageName,
                      Integer current,
                      Integer maximum,
                      Integer starsReward) {
}
