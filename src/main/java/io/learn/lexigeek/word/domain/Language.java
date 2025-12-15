package io.learn.lexigeek.word.domain;

import io.learn.lexigeek.common.entity.AbstractUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

    @Column(name = "name", nullable = false, length = 20)
    private String name;

    @Column(name = "shortcut", nullable = false, length = 10)
    private String shortcut;

    @Column(name = "code_for_translator", nullable = false, length = 10)
    private String codeForTranslator;

    @Column(name = "public", nullable = false)
    private boolean isPublic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;
}
