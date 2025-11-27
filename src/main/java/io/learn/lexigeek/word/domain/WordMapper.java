package io.learn.lexigeek.word.domain;

import io.learn.lexigeek.word.dto.WordDto;
import io.learn.lexigeek.word.dto.WordForm;
import io.learn.lexigeek.word.dto.WordPartDto;
import io.learn.lexigeek.word.dto.WordPartForm;
import io.learn.lexigeek.word.dto.WordStatsDto;
import lombok.experimental.UtilityClass;

import java.util.stream.Collectors;

@UtilityClass
class WordMapper {

    Word formToEntity(final WordForm form) {
        final Word word = new Word();
        word.setMechanism(form.mechanism());
        word.setComment(form.comment());
        word.setChosen(false);
        word.setToRepeat(false);
        word.setAccepted(false);

        form.wordParts().forEach(partForm -> {
            final WordPart wordPart = wordPartFormToEntity(partForm);
            word.addWordPart(wordPart);
        });

        return word;
    }

    void updateEntityFromForm(final Word word, final WordForm form) {
        word.setMechanism(form.mechanism());
        word.setComment(form.comment());

        word.getWordParts().clear();
        if (form.wordParts() != null) {
            form.wordParts().forEach(partForm -> {
                final WordPart wordPart = wordPartFormToEntity(partForm);
                word.addWordPart(wordPart);
            });
        }
    }

    WordPart wordPartFormToEntity(final WordPartForm form) {
        final WordPart wordPart = new WordPart();
        wordPart.setAnswer(form.answer());
        wordPart.setBasicWord(form.basicWord());
        wordPart.setPosition(form.position());
        wordPart.setToSpeech(form.toSpeech());
        wordPart.setSeparator(form.separator());
        wordPart.setSeparatorType(form.separatorType());
        wordPart.setWord(form.word());
        return wordPart;
    }

    WordDto entityToDto(final Word word) {
        return new WordDto(
                word.getUuid(),
                word.getAccepted(),
                word.getChosen(),
                word.getComment(),
                word.getCreated(),
                word.getLastTimeRepeated(),
                word.getMechanism(),
                word.getRepeated(),
                word.getResetTime(),
                word.getToRepeat(),
                word.getWordParts().stream()
                        .sorted((a, b) -> a.getPosition().compareTo(b.getPosition()))
                        .map(WordMapper::wordPartEntityToDto)
                        .collect(Collectors.toList()),
                word.getCategories().stream()
                        .map(Category::getName)
                        .collect(Collectors.toSet())
        );
    }

    WordPartDto wordPartEntityToDto(final WordPart wordPart) {
        return new WordPartDto(
                wordPart.getUuid(),
                wordPart.getAnswer(),
                wordPart.getBasicWord(),
                wordPart.getPosition(),
                wordPart.getToSpeech(),
                wordPart.getSeparator(),
                wordPart.getSeparatorType(),
                wordPart.getWord()
        );
    }

    WordStatsDto wordStatsEntityToDto(final WordStats wordStats) {
        return new WordStatsDto(
                wordStats.getUuid(),
                wordStats.getCorrect(),
                wordStats.getMethod(),
                wordStats.getAnswerTime()
        );
    }
}

