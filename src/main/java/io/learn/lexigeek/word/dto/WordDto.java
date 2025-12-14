package io.learn.lexigeek.word.dto;

import io.learn.lexigeek.word.domain.WordMechanism;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record WordDto(UUID uuid,
                      Boolean accepted,
                      Boolean chosen,
                      String comment,
                      LocalDateTime created,
                      LocalDateTime lastTimeRepeated,
                      WordMechanism mechanism,
                      Integer repeated,
                      LocalDateTime resetTime,
                      List<WordPartDto> wordParts,
                      Set<String> categoryNames) {
}
