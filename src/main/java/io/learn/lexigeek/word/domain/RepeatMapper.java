package io.learn.lexigeek.word.domain;

import io.learn.lexigeek.category.domain.CategoryMode;
import io.learn.lexigeek.word.dto.RepeatSessionDto;
import io.learn.lexigeek.word.dto.RepeatWordDto;
import io.learn.lexigeek.word.dto.RepeatWordMethod;
import io.learn.lexigeek.word.dto.WordPartDto;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
class RepeatMapper {

    static RepeatSessionDto sessionToDto(final RepeatSession session) {
        return new RepeatSessionDto(
                session.getUuid(),
                session.getLanguage().getUuid(),
                session.getWordQueue().size(),
                session.getMethod(),
                session.getCreated()
        );
    }

    static RepeatWordDto wordToRepeatDto(final Word word, final RepeatWordMethod method, final CategoryMode categoryMode) {
        final List<WordPartDto> wordPartDtos = word.getWordParts().stream()
                .sorted((wp1, wp2) -> wp1.getPosition().compareTo(wp2.getPosition()))
                .map(WordMapper::wordPartEntityToDto)
                .collect(Collectors.toList());

        return new RepeatWordDto(
                null, // No attempt UUID needed
                word.getUuid(),
                word.getComment(),
                word.getMechanism(),
                wordPartDtos,
                method,
                categoryMode
        );
    }
}

