package io.learn.lexigeek.task.domain;

import io.learn.lexigeek.common.entity.AbstractUuidEntity;
import io.learn.lexigeek.task.dto.TaskType;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

@Entity
@Table(name = "tasks")
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Setter
@Getter
@FieldNameConstants
class Task extends AbstractUuidEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private TaskType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "language_id", nullable = false)
    private Language language;

    @Column(name = "current", nullable = false)
    private Integer current = 0;

    @Column(name = "maximum", nullable = false)
    private Integer maximum;

    @Column(name = "stars_reward", nullable = false)
    private Integer starsReward;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
}
