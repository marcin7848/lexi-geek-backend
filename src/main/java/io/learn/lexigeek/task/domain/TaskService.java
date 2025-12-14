package io.learn.lexigeek.task.domain;

import io.learn.lexigeek.account.AccountFacade;
import io.learn.lexigeek.account.dto.AccountDto;
import io.learn.lexigeek.common.exception.NotFoundException;
import io.learn.lexigeek.common.validation.ErrorCodes;
import io.learn.lexigeek.task.TaskFacade;
import io.learn.lexigeek.task.dto.TaskConfigDto;
import io.learn.lexigeek.task.dto.TaskDto;
import io.learn.lexigeek.task.dto.TaskScheduleDto;
import io.learn.lexigeek.task.dto.TaskSettingsDto;
import io.learn.lexigeek.task.dto.TaskType;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class TaskService implements TaskFacade {

    private final TaskRepository taskRepository;
    private final TaskSettingsRepository taskSettingsRepository;
    private final TaskScheduleRepository taskScheduleRepository;
    private final AccountRepository accountRepository;
    private final LanguageRepository languageRepository;
    private final AccountFacade accountFacade;

    @Override
    public List<TaskDto> getTasks() {
        final AccountDto accountDto = accountFacade.getLoggedAccount();
        final List<Task> tasks = taskRepository.findAllByAccountId(accountDto.id());
        return tasks.stream()
                .map(TaskMapper::entityToDto)
                .toList();
    }

    @Override
    @Transactional
    public List<TaskDto> reloadTasks() {
        final AccountDto accountDto = accountFacade.getLoggedAccount();
        return reloadTasks(accountDto);
    }

    @Override
    @Transactional
    public List<TaskDto> reloadTasks(final AccountDto accountDto) {
        taskRepository.deleteAllByAccountId(accountDto.id());
        final List<TaskSettings> allSettings = taskSettingsRepository.findAllByAccountId(accountDto.id());
        final Account account = accountRepository.findById(accountDto.id())
                .orElseThrow(() -> new NotFoundException(ErrorCodes.USER_NOT_FOUND, accountDto.uuid()));
        final List<Task> newTasks = new ArrayList<>();
        for (final TaskSettings settings : allSettings) {
            newTasks.addAll(generateTasksForLanguage(settings, account));
        }
        taskRepository.saveAll(newTasks);
        return newTasks.stream()
                .map(TaskMapper::entityToDto)
                .toList();
    }

    private List<Task> generateTasksForLanguage(final TaskSettings settings, final Account account) {
        final List<Task> tasks = new ArrayList<>();
        final Language language = settings.getLanguage();
        if (settings.getRepeatDictionaryEnabled()) {
            final TaskType taskType = TaskType.REPEAT_DICTIONARY;
            tasks.add(createTask(taskType, language, account,
                    settings.getRepeatDictionaryMaximum(), calculateStarsReward(settings.getRepeatDictionaryMaximum(), taskType)));
        }
        if (settings.getRepeatExerciseEnabled()) {
            final TaskType taskType = TaskType.REPEAT_EXERCISE;
            tasks.add(createTask(taskType, language, account,
                    settings.getRepeatExerciseMaximum(), calculateStarsReward(settings.getRepeatExerciseMaximum(), taskType)));
        }
        if (settings.getAddDictionaryEnabled()) {
            final TaskType taskType = TaskType.ADD_DICTIONARY;
            tasks.add(createTask(taskType, language, account,
                    settings.getAddDictionaryMaximum(), calculateStarsReward(settings.getAddDictionaryMaximum(), taskType)));
        }
        if (settings.getAddExerciseEnabled()) {
            final TaskType taskType = TaskType.ADD_EXERCISE;
            tasks.add(createTask(taskType, language, account,
                    settings.getAddExerciseMaximum(), calculateStarsReward(settings.getAddExerciseMaximum(), taskType)));
        }
        return tasks;
    }

    private Task createTask(final TaskType type, final Language language, final Account account,
                            final Integer maximum, final Integer starsReward) {
        final Task task = new Task();
        task.setType(type);
        task.setLanguage(language);
        task.setAccount(account);
        task.setCurrent(0);
        task.setMaximum(maximum);
        task.setStarsReward(starsReward);
        return task;
    }

    private Integer calculateStarsReward(final Integer maximum, final TaskType taskType) {
        switch (taskType) {
            case REPEAT_DICTIONARY -> {
                return Math.max(1, maximum / 20);
            }
            case REPEAT_EXERCISE -> {
                return Math.max(2, maximum / 10);
            }
            case ADD_DICTIONARY -> {
                return Math.max(3, maximum / 8);
            }
            case ADD_EXERCISE -> {
                return Math.max(4, maximum / 5);
            }
            default -> {
                return 1;
            }
        }
    }

    @Override
    public TaskConfigDto getTaskConfig() {
        final AccountDto accountDto = accountFacade.getLoggedAccount();

        final List<TaskSettings> settings = taskSettingsRepository.findAllByAccountId(accountDto.id());
        final List<TaskSettingsDto> settingsDtos = settings.stream()
                .map(TaskMapper::settingsEntityToDto)
                .collect(Collectors.toList());

        final TaskSchedule schedule = taskScheduleRepository.findByAccountId(accountDto.id())
                .orElseGet(() -> createDefaultSchedule(accountDto));
        final TaskScheduleDto scheduleDto = TaskMapper.scheduleEntityToDto(schedule);

        return new TaskConfigDto(settingsDtos, scheduleDto);
    }

    @Override
    @Transactional
    public void updateTaskConfig(final TaskConfigDto config) {
        final AccountDto accountDto = accountFacade.getLoggedAccount();

        for (final TaskSettingsDto dto : config.settings()) {
            final Language language = languageRepository.findByUuid(dto.languageUuid())
                    .orElseThrow(() -> new NotFoundException(ErrorCodes.LANGUAGE_NOT_FOUND, dto.languageUuid()));
            final TaskSettings settings = taskSettingsRepository
                    .findByLanguageIdAndAccountId(language.getId(), accountDto.id())
                    .orElseGet(() -> createDefaultSettings(language, accountDto));
            TaskMapper.updateSettingsFromDto(settings, dto);
            taskSettingsRepository.save(settings);
        }

        final TaskSchedule schedule = taskScheduleRepository.findByAccountId(accountDto.id())
                .orElseGet(() -> createDefaultSchedule(accountDto));
        TaskMapper.updateScheduleFromDto(schedule, config.schedule());
        taskScheduleRepository.save(schedule);
    }

    private TaskSettings createDefaultSettings(final Language language, final AccountDto accountDto) {
        final TaskSettings settings = new TaskSettings();
        settings.setLanguage(language);
        final Account account = accountRepository.findById(accountDto.id())
                .orElseThrow(() -> new NotFoundException(ErrorCodes.USER_NOT_FOUND, accountDto.id()));
        settings.setAccount(account);
        return settings;
    }


    private TaskSchedule createDefaultSchedule(final AccountDto accountDto) {
        final TaskSchedule schedule = new TaskSchedule();
        final Account account = accountRepository.findById(accountDto.id())
                .orElseThrow(() -> new NotFoundException(ErrorCodes.USER_NOT_FOUND, accountDto.id()));
        schedule.setAccount(account);
        return schedule;
    }
}
