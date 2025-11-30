package io.learn.lexigeek.word.domain;

import io.learn.lexigeek.common.entity.AbstractUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.FieldNameConstants;

@Entity(name = "WordLanguage")
@Table(name = "languages")
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Getter
@FieldNameConstants
class Language extends AbstractUuidEntity {

    @Column(name = "shortcut", nullable = false, length = 10)
    private String shortcut;
}
