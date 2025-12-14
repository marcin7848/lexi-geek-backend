package io.learn.lexigeek.task.dto;

import java.util.UUID;

public record TaskSettingsDto(UUID languageUuid,
                              TaskTypeSettings repeatDictionary,
                              TaskTypeSettings repeatExercise,
                              TaskTypeSettings addDictionary,
                              TaskTypeSettings addExercise) {
}
