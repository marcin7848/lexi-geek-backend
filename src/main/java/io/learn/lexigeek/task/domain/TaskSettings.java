package io.learn.lexigeek.task.domain;

import io.learn.lexigeek.common.entity.AbstractUuidEntity;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

@Entity
@Table(name = "task_settings")
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Setter
@Getter
@FieldNameConstants
class TaskSettings extends AbstractUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "language_id", nullable = false)
    private Language language;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "repeat_dictionary_enabled", nullable = false)
    private Boolean repeatDictionaryEnabled = true;

    @Column(name = "repeat_dictionary_maximum", nullable = false)
    private Integer repeatDictionaryMaximum = 30;

    @Column(name = "repeat_exercise_enabled", nullable = false)
    private Boolean repeatExerciseEnabled = true;

    @Column(name = "repeat_exercise_maximum", nullable = false)
    private Integer repeatExerciseMaximum = 30;

    @Column(name = "add_dictionary_enabled", nullable = false)
    private Boolean addDictionaryEnabled = true;

    @Column(name = "add_dictionary_maximum", nullable = false)
    private Integer addDictionaryMaximum = 10;

    @Column(name = "add_exercise_enabled", nullable = false)
    private Boolean addExerciseEnabled = false;

    @Column(name = "add_exercise_maximum", nullable = false)
    private Integer addExerciseMaximum = 10;
}
