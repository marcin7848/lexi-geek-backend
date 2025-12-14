package io.learn.lexigeek.task.domain;

import io.learn.lexigeek.common.entity.AbstractUuidEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity(name = "TaskLanguage")
@Table(name = "languages")
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Getter
@Setter
class Language extends AbstractUuidEntity {

    @Column(name = "name", nullable = false, length = 20)
    private String name;

    @OneToMany(mappedBy = "language", cascade = CascadeType.ALL, orphanRemoval = true)
    @Setter(AccessLevel.NONE)
    private List<Task> tasks = new ArrayList<>();

    @OneToMany(mappedBy = "language", cascade = CascadeType.ALL, orphanRemoval = true)
    @Setter(AccessLevel.NONE)
    private List<TaskSettings> taskSettings = new ArrayList<>();
}
