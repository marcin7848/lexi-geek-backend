package io.learn.lexigeek.word.domain;

import io.learn.lexigeek.category.domain.CategoryMode;
import io.learn.lexigeek.word.dto.RepeatSessionDto;
import io.learn.lexigeek.word.dto.RepeatWordDto;
import io.learn.lexigeek.word.dto.WordPartDto;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Comparator;
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

    static RepeatWordDto wordToRepeatDto(final Word word, final WordMethod method, final CategoryMode categoryMode) {
        final List<WordPartDto> wordPartDtos = word.getWordParts().stream()
                .sorted(Comparator.comparing(WordPart::getPosition))
                .map(WordMapper::wordPartEntityToDto)
                .collect(Collectors.toList());

        return new RepeatWordDto(
                word.getUuid(),
                word.getComment(),
                word.getMechanism(),
                wordPartDtos,
                method,
                categoryMode
        );
    }
}
