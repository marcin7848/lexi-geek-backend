package io.learn.lexigeek.word.domain;

import io.learn.lexigeek.category.domain.CategoryMethod;
import io.learn.lexigeek.common.entity.AbstractUuidEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "words")
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Setter
@Getter
@FieldNameConstants
class Word extends AbstractUuidEntity {

    @Column(name = "accepted", nullable = false)
    private Boolean accepted = false;

    @Column(name = "chosen", nullable = false)
    private Boolean chosen = false;

    @Column(name = "comment")
    private String comment;

    @Column(name = "created", nullable = false)
    private LocalDateTime created = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "mechanism", nullable = false, length = 5)
    private WordMechanism mechanism;

    @Column(name = "reset_time")
    private LocalDateTime resetTime = LocalDateTime.now();

    @OneToMany(mappedBy = "wordEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WordPart> wordParts = new ArrayList<>();

    @OneToMany(mappedBy = "word", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WordStats> wordStats = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "category_word",
            joinColumns = @JoinColumn(name = "word_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories = new HashSet<>();

    public void addWordPart(final WordPart wordPart) {
        wordParts.add(wordPart);
        wordPart.setWordEntity(this);
    }

    public void removeWordPart(final WordPart wordPart) {
        wordParts.remove(wordPart);
        wordPart.setWordEntity(null);
    }

    public void addWordStats(final WordStats stats) {
        wordStats.add(stats);
        stats.setWord(this);
    }

    public void removeWordStats(final WordStats stats) {
        wordStats.remove(stats);
        stats.setWord(null);
    }

    public void addCategory(final Category category) {
        categories.add(category);
    }

    public void removeCategory(final Category category) {
        categories.remove(category);
    }

    public CategoryMethod getCategoryMethod() {
        final Set<CategoryMethod> methods = categories.stream()
                .map(Category::getMethod)
                .collect(Collectors.toSet());

        if (methods.size() == 1) {
            return methods.iterator().next();
        }

        return methods.contains(CategoryMethod.QUESTION_TO_ANSWER)
                ? CategoryMethod.QUESTION_TO_ANSWER
                : CategoryMethod.ANSWER_TO_QUESTION;
    }
}

