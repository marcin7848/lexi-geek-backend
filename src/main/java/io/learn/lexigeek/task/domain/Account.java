package io.learn.lexigeek.task.domain;

import io.learn.lexigeek.common.entity.AbstractUuidEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity(name = "TaskAccount")
@Table(name = "accounts")
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Getter
class Account extends AbstractUuidEntity {

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    @Setter(AccessLevel.NONE)
    private List<Task> tasks = new ArrayList<>();

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    @Setter(AccessLevel.NONE)
    private List<TaskSettings> taskSettings = new ArrayList<>();

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    @Setter(AccessLevel.NONE)
    private List<TaskSchedule> taskSchedules = new ArrayList<>();
}
