package io.learn.lexigeek.task.domain;

import io.learn.lexigeek.task.dto.TaskDto;
import io.learn.lexigeek.task.dto.TaskScheduleDto;
import io.learn.lexigeek.task.dto.TaskSettingsDto;
import io.learn.lexigeek.task.dto.TaskTypeSettings;
import lombok.experimental.UtilityClass;

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
        return new TaskScheduleDto(
                schedule.getHour(),
                schedule.getMinute(),
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
        schedule.setHour(dto.hour());
        schedule.setMinute(dto.minute());
        schedule.setFrequency(dto.frequency());
        schedule.setFrequencyValue(dto.frequencyValue());
        // Note: lastRunAt is not updated from DTO - it's managed by the system
    }
}
