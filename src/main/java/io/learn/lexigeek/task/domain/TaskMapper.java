package io.learn.lexigeek.task.domain;

import io.learn.lexigeek.common.utils.DateTimeUtils;
import io.learn.lexigeek.task.dto.TaskDto;
import io.learn.lexigeek.task.dto.TaskScheduleDto;
import io.learn.lexigeek.task.dto.TaskSettingsDto;
import io.learn.lexigeek.task.dto.TaskTypeSettings;
import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;

@UtilityClass
class TaskMapper {

    TaskDto entityToDto(final Task task) {
        return new TaskDto(
                task.getUuid(),
                task.getType(),
                task.getLanguage().getUuid(),
                task.getLanguage().getName(),
                task.getCurrent(),
                task.getMaximum(),
                task.getStarsReward()
        );
    }

    TaskSettingsDto settingsEntityToDto(final TaskSettings settings) {
        return new TaskSettingsDto(
                settings.getLanguage().getUuid(),
                new TaskTypeSettings(settings.getRepeatDictionaryEnabled(), settings.getRepeatDictionaryMaximum()),
                new TaskTypeSettings(settings.getRepeatExerciseEnabled(), settings.getRepeatExerciseMaximum()),
                new TaskTypeSettings(settings.getAddDictionaryEnabled(), settings.getAddDictionaryMaximum()),
                new TaskTypeSettings(settings.getAddExerciseEnabled(), settings.getAddExerciseMaximum())
        );
    }

    TaskScheduleDto scheduleEntityToDto(final TaskSchedule schedule) {
        final LocalDateTime utcDateTime = LocalDateTime.now()
                .withHour(schedule.getHour())
                .withMinute(schedule.getMinute());
        final LocalDateTime warsawDateTime = DateTimeUtils.toLocalDateTime(
                utcDateTime.toInstant(java.time.ZoneOffset.UTC)
        );

        return new TaskScheduleDto(
                warsawDateTime.getHour(),
                warsawDateTime.getMinute(),
                schedule.getFrequency(),
                schedule.getFrequencyValue(),
                schedule.getLastRunAt()
        );
    }

    void updateSettingsFromDto(final TaskSettings settings, final TaskSettingsDto dto) {
        settings.setRepeatDictionaryEnabled(dto.repeatDictionary().enabled());
        settings.setRepeatDictionaryMaximum(dto.repeatDictionary().maximum());
        settings.setRepeatExerciseEnabled(dto.repeatExercise().enabled());
        settings.setRepeatExerciseMaximum(dto.repeatExercise().maximum());
        settings.setAddDictionaryEnabled(dto.addDictionary().enabled());
        settings.setAddDictionaryMaximum(dto.addDictionary().maximum());
        settings.setAddExerciseEnabled(dto.addExercise().enabled());
        settings.setAddExerciseMaximum(dto.addExercise().maximum());
    }

    void updateScheduleFromDto(final TaskSchedule schedule, final TaskScheduleDto dto) {
        final LocalDateTime warsawDateTime = LocalDateTime.now()
                .withHour(dto.hour())
                .withMinute(dto.minute());

        final LocalDateTime utcDateTime = DateTimeUtils.toUTCDateTimeFromDefaultDateTime(warsawDateTime);

        schedule.setHour(utcDateTime.getHour());
        schedule.setMinute(utcDateTime.getMinute());
        schedule.setFrequency(dto.frequency());
        schedule.setFrequencyValue(dto.frequencyValue());
    }
}
