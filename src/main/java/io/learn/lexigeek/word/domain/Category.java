package io.learn.lexigeek.word.domain;

import io.learn.lexigeek.category.domain.CategoryMethod;
import io.learn.lexigeek.category.domain.CategoryMode;
import io.learn.lexigeek.common.entity.AbstractUuidEntity;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

@Entity(name = "WordCategory")
@Table(name = "categories")
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Getter
class Category extends AbstractUuidEntity {

    @ManyToMany(mappedBy = "categories")
    private Set<Word> words = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 10)
    private CategoryMode mode;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 18)
    private CategoryMethod method;

    @Column(name = "name")
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "language_id")
    private Language language;
}
