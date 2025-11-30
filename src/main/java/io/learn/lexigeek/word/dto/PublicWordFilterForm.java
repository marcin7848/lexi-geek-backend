package io.learn.lexigeek.word.dto;

import io.learn.lexigeek.word.domain.WordMechanism;

public record PublicWordFilterForm(WordMechanism mechanism,
                                   String searchText,
                                   String categoryName) {
}
