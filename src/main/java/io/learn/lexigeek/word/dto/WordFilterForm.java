package io.learn.lexigeek.word.dto;

import io.learn.lexigeek.word.domain.WordMechanism;

import java.util.UUID;

public record WordFilterForm(UUID uuid,
                             Boolean accepted,
                             Boolean chosen,
                             String searchText,
                             WordMechanism mechanism) {
}
