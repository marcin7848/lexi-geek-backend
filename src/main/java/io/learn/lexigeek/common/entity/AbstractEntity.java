package io.learn.lexigeek.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

@MappedSuperclass
@Getter
public abstract class AbstractEntity {

    @Id
    @Column(name = "id", unique = true, nullable = false)
    private Long id;
}
