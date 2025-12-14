package io.learn.lexigeek.task.domain;

import io.learn.lexigeek.common.entity.AbstractUuidEntity;
import io.learn.lexigeek.task.dto.TaskFrequency;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import java.time.LocalDateTime;

@Entity
@Table(name = "task_schedules")
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Setter
@Getter
@FieldNameConstants
class TaskSchedule extends AbstractUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "hour", nullable = false)
    private Integer hour = 0;

    @Column(name = "minute", nullable = false)
    private Integer minute = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 50)
    private TaskFrequency frequency = TaskFrequency.DAILY;

    @Column(name = "frequency_value")
    private Integer frequencyValue;

    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;
}
