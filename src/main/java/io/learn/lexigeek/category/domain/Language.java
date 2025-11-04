package io.learn.lexigeek.category.domain;

import io.learn.lexigeek.common.entity.AbstractUuidEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity(name = "CategoryLanguage")
@Table(name = "languages")
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Getter
class Language extends AbstractUuidEntity {

    @OneToMany(
            mappedBy = "language",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Setter(AccessLevel.NONE)
    private List<Category> categories = new ArrayList<>();
}

