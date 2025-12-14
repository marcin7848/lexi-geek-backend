package io.learn.lexigeek.task.domain;

import io.learn.lexigeek.common.entity.AbstractUuidEntity;
import io.learn.lexigeek.task.dto.TaskFrequency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

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
}
