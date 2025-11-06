package io.learn.lexigeek.word.domain;

import io.learn.lexigeek.common.entity.AbstractUuidEntity;
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
@Table(name = "word_parts")
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Setter
@Getter
@FieldNameConstants
class WordPart extends AbstractUuidEntity {

    @Column(name = "answer", nullable = false)
    private Boolean answer = false;

    @Column(name = "basic_word")
    private String basicWord;

    @Column(name = "position", nullable = false)
    private Integer position;

    @Column(name = "to_speech", nullable = false)
    private Boolean toSpeech = false;

    @Column(name = "separator", nullable = false)
    private Boolean separator = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "separator_type", length = 10)
    private SeparatorType separatorType;

    @Column(name = "word")
    private String word;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "word_id", nullable = false)
    private Word wordEntity;
}

