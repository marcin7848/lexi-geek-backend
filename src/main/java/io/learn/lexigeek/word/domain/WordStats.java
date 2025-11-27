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

import java.time.LocalDateTime;

@Entity
@Table(name = "word_stats")
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Setter
@Getter
@FieldNameConstants
class WordStats extends AbstractUuidEntity {

    @Column(name = "correct", nullable = false)
    private Boolean correct;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 18)
    private WordMethod method;

    @Column(name = "answer_time", nullable = false)
    private LocalDateTime answerTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "word_id", nullable = false)
    private Word word;
}

