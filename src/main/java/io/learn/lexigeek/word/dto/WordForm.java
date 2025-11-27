package io.learn.lexigeek.word.dto;

import io.learn.lexigeek.word.domain.WordMechanism;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record WordForm(String comment,
                       @NotNull WordMechanism mechanism,
                       @Valid @NotNull @Size(min = 2) List<WordPartForm> wordParts) { //TODO: dodac walidacje z position, ze sa pokolei
}
