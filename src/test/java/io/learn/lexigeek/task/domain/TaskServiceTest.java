package io.learn.lexigeek.task.domain;

import io.learn.lexigeek.account.AccountFacade;
import io.learn.lexigeek.account.dto.AccountDto;
import io.learn.lexigeek.common.exception.NotFoundException;
import io.learn.lexigeek.common.validation.ErrorCodes;
import io.learn.lexigeek.task.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class TaskServiceTest {

    private final TaskRepository taskRepository = mock(TaskRepository.class);
    private final TaskSettingsRepository taskSettingsRepository = mock(TaskSettingsRepository.class);
    private final TaskScheduleRepository taskScheduleRepository = mock(TaskScheduleRepository.class);
    private final AccountRepository accountRepository = mock(AccountRepository.class);
    private final LanguageRepository languageRepository = mock(LanguageRepository.class);
    private final AccountFacade accountFacade = mock(AccountFacade.class);

    private final TaskService taskService = new TaskService(
            taskRepository,
            taskSettingsRepository,
            taskScheduleRepository,
            accountRepository,
            languageRepository,
            accountFacade
    );

    private UUID accountUuid;
    private UUID languageUuid;
    private Long accountId;
    private Long languageId;
    private AccountDto accountDto;
    private Account account;
    private Language language;

    @BeforeEach
    void setUp() {
        accountUuid = UUID.randomUUID();
        languageUuid = UUID.randomUUID();
        accountId = 1L;
        languageId = 2L;

        accountDto = new AccountDto(accountId, accountUuid, "TestUser", "test@example.com", "password");

        account = mock(Account.class);
        when(account.getId()).thenReturn(accountId);
        when(account.getUuid()).thenReturn(accountUuid);

        language = mock(Language.class);
        when(language.getId()).thenReturn(languageId);
        when(language.getUuid()).thenReturn(languageUuid);
        when(language.getName()).thenReturn("English");
    }

    @Nested
    class GetTasksTests {

        @Test
        void success_returnsUserTasks() {
            // Given
            when(accountFacade.getLoggedAccount()).thenReturn(accountDto);

            Task task1 = createTask(TaskType.REPEAT_DICTIONARY, 10, 30, 2);
            Task task2 = createTask(TaskType.ADD_DICTIONARY, 5, 10, 3);
            List<Task> tasks = List.of(task1, task2);

            when(taskRepository.findAllByAccountId(accountId)).thenReturn(tasks);

            // When
            List<TaskDto> result = taskService.getTasks();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).type()).isEqualTo(TaskType.REPEAT_DICTIONARY);
            assertThat(result.get(0).current()).isEqualTo(10);
            assertThat(result.get(0).maximum()).isEqualTo(30);
            assertThat(result.get(1).type()).isEqualTo(TaskType.ADD_DICTIONARY);

            verify(accountFacade).getLoggedAccount();
            verify(taskRepository).findAllByAccountId(accountId);
        }

        @Test
        void whenNoTasks_returnsEmptyList() {
            // Given
            when(accountFacade.getLoggedAccount()).thenReturn(accountDto);
            when(taskRepository.findAllByAccountId(accountId)).thenReturn(Collections.emptyList());

            // When
            List<TaskDto> result = taskService.getTasks();

            // Then
            assertThat(result).isEmpty();
            verify(taskRepository).findAllByAccountId(accountId);
        }
    }

    @Nested
    class ReloadTasksTests {

        @Test
        void success_deletesOldAndCreatesNewTasks() {
            // Given
            when(accountFacade.getLoggedAccount()).thenReturn(accountDto);
            when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

            Task oldTask = createTask(TaskType.REPEAT_DICTIONARY, 15, 30, 2);
            when(taskRepository.findAllByAccountId(accountId)).thenReturn(List.of(oldTask));

            TaskSettings settings = createTaskSettings();
            when(taskSettingsRepository.findAllByAccountId(accountId)).thenReturn(List.of(settings));

            ArgumentCaptor<List<Task>> taskCaptor = ArgumentCaptor.forClass(List.class);
            when(taskRepository.saveAll(taskCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            List<TaskDto> result = taskService.reloadTasks();

            // Then
            assertThat(result).isNotEmpty();
            verify(taskRepository).deleteAllByAccountId(accountId);
            verify(taskRepository).saveAll(any());

            List<Task> savedTasks = taskCaptor.getValue();
            assertThat(savedTasks).hasSize(3); // 3 enabled task types in default settings
            assertThat(savedTasks).extracting(Task::getCurrent).allMatch(current -> current == 0);
        }

        @Test
        void success_awardsStarsForCompletedTasks() {
            // Given
            when(accountFacade.getLoggedAccount()).thenReturn(accountDto);
            when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

            Task completedTask = createTask(TaskType.REPEAT_DICTIONARY, 30, 30, 5);
            when(taskRepository.findAllByAccountId(accountId)).thenReturn(List.of(completedTask));

            when(taskSettingsRepository.findAllByAccountId(accountId)).thenReturn(Collections.emptyList());
            when(taskRepository.saveAll(any())).thenReturn(Collections.emptyList());

            // When
            taskService.reloadTasks();

            // Then
            verify(accountFacade).addStars(accountDto, 5);
            verify(taskRepository).deleteAllByAccountId(accountId);
        }

        @Test
        void success_awardsStarsForMultipleCompletedTasks() {
            // Given
            when(accountFacade.getLoggedAccount()).thenReturn(accountDto);
            when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

            Task completedTask1 = createTask(TaskType.REPEAT_DICTIONARY, 30, 30, 5);
            Task completedTask2 = createTask(TaskType.ADD_DICTIONARY, 10, 10, 3);
            Task incompleteTask = createTask(TaskType.REPEAT_EXERCISE, 15, 30, 2);
            when(taskRepository.findAllByAccountId(accountId)).thenReturn(List.of(completedTask1, completedTask2, incompleteTask));

            when(taskSettingsRepository.findAllByAccountId(accountId)).thenReturn(Collections.emptyList());
            when(taskRepository.saveAll(any())).thenReturn(Collections.emptyList());

            // When
            taskService.reloadTasks();

            // Then
            verify(accountFacade).addStars(accountDto, 8); // 5 + 3
        }

        @Test
        void success_doesNotAwardStarsWhenNoTasksCompleted() {
            // Given
            when(accountFacade.getLoggedAccount()).thenReturn(accountDto);
            when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

            Task incompleteTask = createTask(TaskType.REPEAT_DICTIONARY, 15, 30, 2);
            when(taskRepository.findAllByAccountId(accountId)).thenReturn(List.of(incompleteTask));

            when(taskSettingsRepository.findAllByAccountId(accountId)).thenReturn(Collections.emptyList());
            when(taskRepository.saveAll(any())).thenReturn(Collections.emptyList());

            // When
            taskService.reloadTasks();

            // Then
            verify(accountFacade, never()).addStars(any(), anyInt());
        }

        @Test
        void success_createsTasksBasedOnSettings() {
            // Given
            when(accountFacade.getLoggedAccount()).thenReturn(accountDto);
            when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
            when(taskRepository.findAllByAccountId(accountId)).thenReturn(Collections.emptyList());

            TaskSettings settings = new TaskSettings();
            settings.setLanguage(language);
            settings.setAccount(account);
            settings.setRepeatDictionaryEnabled(true);
            settings.setRepeatDictionaryMaximum(50);
            settings.setRepeatExerciseEnabled(false);
            settings.setAddDictionaryEnabled(true);
            settings.setAddDictionaryMaximum(20);
            settings.setAddExerciseEnabled(false);

            when(taskSettingsRepository.findAllByAccountId(accountId)).thenReturn(List.of(settings));

            ArgumentCaptor<List<Task>> taskCaptor = ArgumentCaptor.forClass(List.class);
            when(taskRepository.saveAll(taskCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            taskService.reloadTasks();

            // Then
            List<Task> savedTasks = taskCaptor.getValue();
            assertThat(savedTasks).hasSize(2);
            assertThat(savedTasks).extracting(Task::getType)
                    .containsExactlyInAnyOrder(TaskType.REPEAT_DICTIONARY, TaskType.ADD_DICTIONARY);

            Task repeatTask = savedTasks.stream()
                    .filter(t -> t.getType() == TaskType.REPEAT_DICTIONARY)
                    .findFirst()
                    .orElseThrow();
            assertThat(repeatTask.getMaximum()).isEqualTo(50);
        }

        @Test
        void whenAccountNotFound_throwsNotFoundException() {
            // Given
            when(accountFacade.getLoggedAccount()).thenReturn(accountDto);
            when(taskRepository.findAllByAccountId(accountId)).thenReturn(Collections.emptyList());
            when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

            TaskSettings settings = createTaskSettings();
            when(taskSettingsRepository.findAllByAccountId(accountId)).thenReturn(List.of(settings));

            // When & Then
            assertThatThrownBy(() -> taskService.reloadTasks())
                    .isInstanceOf(NotFoundException.class)
                    .hasFieldOrPropertyWithValue("error", ErrorCodes.USER_NOT_FOUND);

            verify(taskRepository, never()).saveAll(any());
        }

        @Test
        void withAccountDto_reloadsTasks() {
            // Given
            when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
            when(taskRepository.findAllByAccountId(accountId)).thenReturn(Collections.emptyList());
            when(taskSettingsRepository.findAllByAccountId(accountId)).thenReturn(Collections.emptyList());
            when(taskRepository.saveAll(any())).thenReturn(Collections.emptyList());

            // When
            List<TaskDto> result = taskService.reloadTasks(accountDto);

            // Then
            assertThat(result).isEmpty();
            verify(taskRepository).deleteAllByAccountId(accountId);
        }
    }

    @Nested
    class GetTaskConfigTests {

        @Test
        void success_returnsConfigWithSettingsAndSchedule() {
            // Given
            when(accountFacade.getLoggedAccount()).thenReturn(accountDto);

            TaskSettings settings = createTaskSettings();
            when(taskSettingsRepository.findAllByAccountId(accountId)).thenReturn(List.of(settings));

            TaskSchedule schedule = createTaskSchedule();
            when(taskScheduleRepository.findByAccountId(accountId)).thenReturn(Optional.of(schedule));

            // When
            TaskConfigDto result = taskService.getTaskConfig();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.settings()).hasSize(1);
            assertThat(result.settings().get(0).languageUuid()).isEqualTo(languageUuid);
            assertThat(result.schedule()).isNotNull();
            // Note: hour may be different due to timezone conversion
            assertThat(result.schedule().frequency()).isEqualTo(TaskFrequency.DAILY);

            verify(taskSettingsRepository).findAllByAccountId(accountId);
            verify(taskScheduleRepository).findByAccountId(accountId);
        }

        @Test
        void success_createsDefaultScheduleIfNotExists() {
            // Given
            when(accountFacade.getLoggedAccount()).thenReturn(accountDto);
            when(taskSettingsRepository.findAllByAccountId(accountId)).thenReturn(Collections.emptyList());
            when(taskScheduleRepository.findByAccountId(accountId)).thenReturn(Optional.empty());
            when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

            // When
            TaskConfigDto result = taskService.getTaskConfig();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.schedule()).isNotNull();
            // Note: hour and minute may be different due to timezone conversion from UTC defaults
            assertThat(result.schedule().frequency()).isEqualTo(TaskFrequency.DAILY);
        }

        @Test
        void success_returnsMultipleLanguageSettings() {
            // Given
            when(accountFacade.getLoggedAccount()).thenReturn(accountDto);

            Language language2 = mock(Language.class);
            when(language2.getId()).thenReturn(3L);
            when(language2.getUuid()).thenReturn(UUID.randomUUID());
            when(language2.getName()).thenReturn("Spanish");

            TaskSettings settings1 = createTaskSettings();
            TaskSettings settings2 = new TaskSettings();
            settings2.setLanguage(language2);
            settings2.setAccount(account);

            when(taskSettingsRepository.findAllByAccountId(accountId))
                    .thenReturn(List.of(settings1, settings2));

            TaskSchedule schedule = createTaskSchedule();
            when(taskScheduleRepository.findByAccountId(accountId)).thenReturn(Optional.of(schedule));

            // When
            TaskConfigDto result = taskService.getTaskConfig();

            // Then
            assertThat(result.settings()).hasSize(2);
        }
    }

    @Nested
    class UpdateTaskConfigTests {

        @Test
        void success_updatesExistingSettings() {
            // Given
            when(accountFacade.getLoggedAccount()).thenReturn(accountDto);
            when(languageRepository.findByUuid(languageUuid)).thenReturn(Optional.of(language));

            TaskSettings existingSettings = createTaskSettings();
            when(taskSettingsRepository.findByLanguageIdAndAccountId(languageId, accountId))
                    .thenReturn(Optional.of(existingSettings));

            TaskSchedule existingSchedule = createTaskSchedule();
            when(taskScheduleRepository.findByAccountId(accountId)).thenReturn(Optional.of(existingSchedule));

            TaskTypeSettings repeatDict = new TaskTypeSettings(true, 40);
            TaskTypeSettings repeatExer = new TaskTypeSettings(true, 35);
            TaskTypeSettings addDict = new TaskTypeSettings(false, 15);
            TaskTypeSettings addExer = new TaskTypeSettings(false, 10);
            TaskSettingsDto settingsDto = new TaskSettingsDto(languageUuid, repeatDict, repeatExer, addDict, addExer);

            TaskScheduleDto scheduleDto = new TaskScheduleDto(10, 30, TaskFrequency.WEEKLY, 1, null);
            TaskConfigDto config = new TaskConfigDto(List.of(settingsDto), scheduleDto);

            // When
            taskService.updateTaskConfig(config);

            // Then
            verify(taskSettingsRepository).save(existingSettings);
            verify(taskScheduleRepository).save(existingSchedule);
            assertThat(existingSettings.getRepeatDictionaryMaximum()).isEqualTo(40);
            assertThat(existingSettings.getAddDictionaryEnabled()).isFalse();
            // Note: hour and minute are converted through timezone conversion, so we just verify the save was called
            assertThat(existingSchedule.getFrequency()).isEqualTo(TaskFrequency.WEEKLY);
        }

        @Test
        void success_createsNewSettingsIfNotExists() {
            // Given
            when(accountFacade.getLoggedAccount()).thenReturn(accountDto);
            when(languageRepository.findByUuid(languageUuid)).thenReturn(Optional.of(language));
            when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

            when(taskSettingsRepository.findByLanguageIdAndAccountId(languageId, accountId))
                    .thenReturn(Optional.empty());

            TaskSchedule existingSchedule = createTaskSchedule();
            when(taskScheduleRepository.findByAccountId(accountId)).thenReturn(Optional.of(existingSchedule));

            TaskTypeSettings repeatDict = new TaskTypeSettings(true, 25);
            TaskTypeSettings repeatExer = new TaskTypeSettings(true, 30);
            TaskTypeSettings addDict = new TaskTypeSettings(true, 10);
            TaskTypeSettings addExer = new TaskTypeSettings(false, 10);
            TaskSettingsDto settingsDto = new TaskSettingsDto(languageUuid, repeatDict, repeatExer, addDict, addExer);

            TaskScheduleDto scheduleDto = new TaskScheduleDto(9, 0, TaskFrequency.DAILY, null, null);
            TaskConfigDto config = new TaskConfigDto(List.of(settingsDto), scheduleDto);

            ArgumentCaptor<TaskSettings> settingsCaptor = ArgumentCaptor.forClass(TaskSettings.class);

            // When
            taskService.updateTaskConfig(config);

            // Then
            verify(taskSettingsRepository).save(settingsCaptor.capture());
            TaskSettings savedSettings = settingsCaptor.getValue();
            assertThat(savedSettings.getLanguage()).isEqualTo(language);
            assertThat(savedSettings.getAccount()).isEqualTo(account);
        }

        @Test
        void success_createsNewScheduleIfNotExists() {
            // Given
            when(accountFacade.getLoggedAccount()).thenReturn(accountDto);
            when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
            when(taskScheduleRepository.findByAccountId(accountId)).thenReturn(Optional.empty());

            TaskScheduleDto scheduleDto = new TaskScheduleDto(12, 0, TaskFrequency.EVERY_N_DAYS, 3, null);
            TaskConfigDto config = new TaskConfigDto(Collections.emptyList(), scheduleDto);

            ArgumentCaptor<TaskSchedule> scheduleCaptor = ArgumentCaptor.forClass(TaskSchedule.class);

            // When
            taskService.updateTaskConfig(config);

            // Then
            verify(taskScheduleRepository).save(scheduleCaptor.capture());
            TaskSchedule savedSchedule = scheduleCaptor.getValue();
            assertThat(savedSchedule.getAccount()).isEqualTo(account);
            assertThat(savedSchedule.getFrequency()).isEqualTo(TaskFrequency.EVERY_N_DAYS);
        }

        @Test
        void success_updatesMultipleLanguageSettings() {
            // Given
            when(accountFacade.getLoggedAccount()).thenReturn(accountDto);

            UUID languageUuid2 = UUID.randomUUID();
            Language language2 = mock(Language.class);
            when(language2.getId()).thenReturn(3L);
            when(language2.getUuid()).thenReturn(languageUuid2);

            when(languageRepository.findByUuid(languageUuid)).thenReturn(Optional.of(language));
            when(languageRepository.findByUuid(languageUuid2)).thenReturn(Optional.of(language2));

            TaskSettings settings1 = createTaskSettings();
            TaskSettings settings2 = new TaskSettings();
            settings2.setLanguage(language2);
            settings2.setAccount(account);

            when(taskSettingsRepository.findByLanguageIdAndAccountId(languageId, accountId))
                    .thenReturn(Optional.of(settings1));
            when(taskSettingsRepository.findByLanguageIdAndAccountId(3L, accountId))
                    .thenReturn(Optional.of(settings2));

            TaskSchedule schedule = createTaskSchedule();
            when(taskScheduleRepository.findByAccountId(accountId)).thenReturn(Optional.of(schedule));

            TaskTypeSettings repeatDict = new TaskTypeSettings(true, 40);
            TaskTypeSettings repeatExer = new TaskTypeSettings(true, 35);
            TaskTypeSettings addDict = new TaskTypeSettings(true, 15);
            TaskTypeSettings addExer = new TaskTypeSettings(false, 10);

            TaskSettingsDto settingsDto1 = new TaskSettingsDto(languageUuid, repeatDict, repeatExer, addDict, addExer);
            TaskSettingsDto settingsDto2 = new TaskSettingsDto(languageUuid2, repeatDict, repeatExer, addDict, addExer);

            TaskScheduleDto scheduleDto = new TaskScheduleDto(9, 0, TaskFrequency.DAILY, null, null);
            TaskConfigDto config = new TaskConfigDto(List.of(settingsDto1, settingsDto2), scheduleDto);

            // When
            taskService.updateTaskConfig(config);

            // Then
            verify(taskSettingsRepository, times(2)).save(any(TaskSettings.class));
        }

        @Test
        void whenLanguageNotFound_throwsNotFoundException() {
            // Given
            when(accountFacade.getLoggedAccount()).thenReturn(accountDto);
            when(languageRepository.findByUuid(languageUuid)).thenReturn(Optional.empty());

            TaskTypeSettings repeatDict = new TaskTypeSettings(true, 30);
            TaskTypeSettings repeatExer = new TaskTypeSettings(true, 30);
            TaskTypeSettings addDict = new TaskTypeSettings(true, 10);
            TaskTypeSettings addExer = new TaskTypeSettings(false, 10);
            TaskSettingsDto settingsDto = new TaskSettingsDto(languageUuid, repeatDict, repeatExer, addDict, addExer);

            TaskScheduleDto scheduleDto = new TaskScheduleDto(9, 0, TaskFrequency.DAILY, null, null);
            TaskConfigDto config = new TaskConfigDto(List.of(settingsDto), scheduleDto);

            // When & Then
            assertThatThrownBy(() -> taskService.updateTaskConfig(config))
                    .isInstanceOf(NotFoundException.class)
                    .hasFieldOrPropertyWithValue("error", ErrorCodes.LANGUAGE_NOT_FOUND);

            verify(taskSettingsRepository, never()).save(any());
        }

        @Test
        void whenAccountNotFoundForNewSettings_throwsNotFoundException() {
            // Given
            when(accountFacade.getLoggedAccount()).thenReturn(accountDto);
            when(languageRepository.findByUuid(languageUuid)).thenReturn(Optional.of(language));
            when(taskSettingsRepository.findByLanguageIdAndAccountId(languageId, accountId))
                    .thenReturn(Optional.empty());
            when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

            TaskTypeSettings repeatDict = new TaskTypeSettings(true, 30);
            TaskTypeSettings repeatExer = new TaskTypeSettings(true, 30);
            TaskTypeSettings addDict = new TaskTypeSettings(true, 10);
            TaskTypeSettings addExer = new TaskTypeSettings(false, 10);
            TaskSettingsDto settingsDto = new TaskSettingsDto(languageUuid, repeatDict, repeatExer, addDict, addExer);

            TaskScheduleDto scheduleDto = new TaskScheduleDto(9, 0, TaskFrequency.DAILY, null, null);
            TaskConfigDto config = new TaskConfigDto(List.of(settingsDto), scheduleDto);

            // When & Then
            assertThatThrownBy(() -> taskService.updateTaskConfig(config))
                    .isInstanceOf(NotFoundException.class)
                    .hasFieldOrPropertyWithValue("error", ErrorCodes.USER_NOT_FOUND);
        }

        @Test
        void whenAccountNotFoundForNewSchedule_throwsNotFoundException() {
            // Given
            when(accountFacade.getLoggedAccount()).thenReturn(accountDto);
            when(taskScheduleRepository.findByAccountId(accountId)).thenReturn(Optional.empty());
            when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

            TaskScheduleDto scheduleDto = new TaskScheduleDto(9, 0, TaskFrequency.DAILY, null, null);
            TaskConfigDto config = new TaskConfigDto(Collections.emptyList(), scheduleDto);

            // When & Then
            assertThatThrownBy(() -> taskService.updateTaskConfig(config))
                    .isInstanceOf(NotFoundException.class)
                    .hasFieldOrPropertyWithValue("error", ErrorCodes.USER_NOT_FOUND);
        }
    }

    @Nested
    class FillTaskTests {

        @Test
        void success_incrementsTaskProgress() {
            // Given
            when(accountFacade.getLoggedAccount()).thenReturn(accountDto);
            when(languageRepository.findByUuid(languageUuid)).thenReturn(Optional.of(language));

            Task task = createTask(TaskType.REPEAT_DICTIONARY, 10, 30, 2);
            when(taskRepository.findByAccountIdAndTypeAndLanguageId(accountId, TaskType.REPEAT_DICTIONARY, languageId))
                    .thenReturn(Optional.of(task));

            // When
            taskService.fillTask(TaskType.REPEAT_DICTIONARY, languageUuid, 5);

            // Then
            assertThat(task.getCurrent()).isEqualTo(15);
            verify(taskRepository).save(task);
        }

        @Test
        void success_incrementsTaskProgressMultipleTimes() {
            // Given
            when(accountFacade.getLoggedAccount()).thenReturn(accountDto);
            when(languageRepository.findByUuid(languageUuid)).thenReturn(Optional.of(language));

            Task task = createTask(TaskType.ADD_DICTIONARY, 5, 10, 3);
            when(taskRepository.findByAccountIdAndTypeAndLanguageId(accountId, TaskType.ADD_DICTIONARY, languageId))
                    .thenReturn(Optional.of(task));

            // When
            taskService.fillTask(TaskType.ADD_DICTIONARY, languageUuid, 2);
            taskService.fillTask(TaskType.ADD_DICTIONARY, languageUuid, 1);

            // Then
            assertThat(task.getCurrent()).isEqualTo(8);
            verify(taskRepository, times(2)).save(task);
        }

        @Test
        void success_allowsProgressOverMaximum() {
            // Given
            when(accountFacade.getLoggedAccount()).thenReturn(accountDto);
            when(languageRepository.findByUuid(languageUuid)).thenReturn(Optional.of(language));

            Task task = createTask(TaskType.REPEAT_EXERCISE, 28, 30, 2);
            when(taskRepository.findByAccountIdAndTypeAndLanguageId(accountId, TaskType.REPEAT_EXERCISE, languageId))
                    .thenReturn(Optional.of(task));

            // When
            taskService.fillTask(TaskType.REPEAT_EXERCISE, languageUuid, 5);

            // Then
            assertThat(task.getCurrent()).isEqualTo(33);
            verify(taskRepository).save(task);
        }

        @Test
        void success_worksForDifferentTaskTypes() {
            // Given
            when(accountFacade.getLoggedAccount()).thenReturn(accountDto);
            when(languageRepository.findByUuid(languageUuid)).thenReturn(Optional.of(language));

            Task exerciseTask = createTask(TaskType.REPEAT_EXERCISE, 0, 30, 2);
            when(taskRepository.findByAccountIdAndTypeAndLanguageId(accountId, TaskType.REPEAT_EXERCISE, languageId))
                    .thenReturn(Optional.of(exerciseTask));

            // When
            taskService.fillTask(TaskType.REPEAT_EXERCISE, languageUuid, 10);

            // Then
            assertThat(exerciseTask.getCurrent()).isEqualTo(10);
            verify(taskRepository).save(exerciseTask);
        }

        @Test
        void whenLanguageNotFound_throwsNotFoundException() {
            // Given
            when(accountFacade.getLoggedAccount()).thenReturn(accountDto);
            when(languageRepository.findByUuid(languageUuid)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> taskService.fillTask(TaskType.REPEAT_DICTIONARY, languageUuid, 5))
                    .isInstanceOf(NotFoundException.class)
                    .hasFieldOrPropertyWithValue("error", ErrorCodes.LANGUAGE_NOT_FOUND);

            verify(taskRepository, never()).findByAccountIdAndTypeAndLanguageId(anyLong(), any(), anyLong());
        }

        @Test
        void whenTaskNotFound_throwsNotFoundException() {
            // Given
            when(accountFacade.getLoggedAccount()).thenReturn(accountDto);
            when(languageRepository.findByUuid(languageUuid)).thenReturn(Optional.of(language));
            when(taskRepository.findByAccountIdAndTypeAndLanguageId(accountId, TaskType.ADD_EXERCISE, languageId))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> taskService.fillTask(TaskType.ADD_EXERCISE, languageUuid, 5))
                    .isInstanceOf(NotFoundException.class)
                    .hasFieldOrPropertyWithValue("error", ErrorCodes.TASK_NOT_FOUND);

            verify(taskRepository, never()).save(any());
        }

        @Test
        void success_incrementsWithOnePoint() {
            // Given
            when(accountFacade.getLoggedAccount()).thenReturn(accountDto);
            when(languageRepository.findByUuid(languageUuid)).thenReturn(Optional.of(language));

            Task task = createTask(TaskType.REPEAT_DICTIONARY, 0, 30, 2);
            when(taskRepository.findByAccountIdAndTypeAndLanguageId(accountId, TaskType.REPEAT_DICTIONARY, languageId))
                    .thenReturn(Optional.of(task));

            // When
            taskService.fillTask(TaskType.REPEAT_DICTIONARY, languageUuid, 1);

            // Then
            assertThat(task.getCurrent()).isEqualTo(1);
            verify(taskRepository).save(task);
        }
    }

    // Helper methods

    private Task createTask(TaskType type, int current, int maximum, int starsReward) {
        Task task = new Task();
        task.setType(type);
        task.setLanguage(language);
        task.setAccount(account);
        task.setCurrent(current);
        task.setMaximum(maximum);
        task.setStarsReward(starsReward);
        return task;
    }

    private TaskSettings createTaskSettings() {
        TaskSettings settings = new TaskSettings();
        settings.setLanguage(language);
        settings.setAccount(account);
        settings.setRepeatDictionaryEnabled(true);
        settings.setRepeatDictionaryMaximum(30);
        settings.setRepeatExerciseEnabled(true);
        settings.setRepeatExerciseMaximum(30);
        settings.setAddDictionaryEnabled(true);
        settings.setAddDictionaryMaximum(10);
        settings.setAddExerciseEnabled(false);
        settings.setAddExerciseMaximum(10);
        return settings;
    }

    private TaskSchedule createTaskSchedule() {
        TaskSchedule schedule = new TaskSchedule();
        schedule.setAccount(account);
        schedule.setHour(9);
        schedule.setMinute(0);
        schedule.setFrequency(TaskFrequency.DAILY);
        schedule.setFrequencyValue(null);
        schedule.setLastRunAt(LocalDateTime.now().minusDays(1));
        return schedule;
    }
}

