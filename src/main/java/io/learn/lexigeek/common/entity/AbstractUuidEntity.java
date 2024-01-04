package io.learn.lexigeek.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;

@MappedSuperclass
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@ToString
@FieldNameConstants
public abstract class AbstractUuidEntity extends AbstractEntity {

    @Column(name = "uuid", unique = true, nullable = false)
    private UUID uuid = UUID.randomUUID();
}
