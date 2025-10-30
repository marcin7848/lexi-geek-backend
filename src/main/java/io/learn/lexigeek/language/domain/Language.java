package io.learn.lexigeek.language.domain;

import io.learn.lexigeek.common.entity.AbstractUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

@Entity
@Table(name = "languages")
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Setter
@Getter
@FieldNameConstants
class Language extends AbstractUuidEntity {

    @Column(name = "name", nullable = false, length = 20)
    private String name;

    @Column(name = "shortcut", nullable = false, length = 10)
    private String shortcut;

    @Column(name = "code_for_speech", nullable = false, length = 10)
    private String codeForSpeech;

    @Column(name = "code_for_translator", nullable = false, length = 10)
    private String codeForTranslator;

    @Column(name = "hidden", nullable = false)
    private boolean hidden;

    @Column(name = "special_letters", nullable = false, length = 255)
    private String specialLetters;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;
}
