
package io.learn.lexigeek.word.domain;

import io.learn.lexigeek.category.CategoryFacade;
import io.learn.lexigeek.common.exception.NotFoundException;
import io.learn.lexigeek.common.validation.ErrorCodes;
import io.learn.lexigeek.word.AutomaticTranslationFacade;
import io.learn.lexigeek.word.WordFacade;
import io.learn.lexigeek.word.dto.AutoTranslateForm;
import io.learn.lexigeek.word.dto.WordForm;
import io.learn.lexigeek.word.dto.WordPartForm;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class AutomaticTranslationService implements AutomaticTranslationFacade {

    private static final String POLISH_LANGUAGE_CODE = "pl";

    private final CategoryFacade categoryFacade;
    private final WordFacade wordFacade;
    private final LanguageRepository languageRepository;
    private final TranslationService translationService;

    @Override
    public void autoTranslate(final UUID languageUuid, final UUID categoryUuid, final AutoTranslateForm form) {
        categoryFacade.verifyCategoryAccess(languageUuid, categoryUuid);

        final Language language = languageRepository.findByUuid(languageUuid)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.LANGUAGE_NOT_FOUND, languageUuid));

        final List<AutomaticTranslationWord> words = splitTextIntoWords(form.text());
        final List<AutomaticTranslationWord> translatedWords = words.parallelStream()
                .map(word -> translate(word, language.getShortcut()))
                .toList();

        final List<WordForm> wordForms = mapToWordForms(translatedWords);
        wordForms.forEach(wordForm -> wordFacade.createWord(languageUuid, categoryUuid, wordForm));
    }

    private List<AutomaticTranslationWord> splitTextIntoWords(final String text) {
        return Arrays.stream(text.split("\\s+"))
                .map(word -> word.replaceAll("[^a-zA-Z0-9]", ""))
                .filter(word -> !word.isEmpty())
                .map(word -> new AutomaticTranslationWord(word, List.of()))
                .toList();
    }

    private List<WordForm> mapToWordForms(final List<AutomaticTranslationWord> translatedWords) {
        return translatedWords.stream()
                .map(tw -> {
                    final List<WordPartForm> wordParts =
                            Stream.concat(
                                    Stream.of(new WordPartForm(false, null, 0, false, false, null, tw.question())),
                                    IntStream.range(0, tw.answers().size())
                                            .mapToObj(i -> new WordPartForm(true, null, i + 1, true, false, null, tw.answers().get(i)))
                            ).toList();

                    return new WordForm(null, WordMechanism.BASIC, wordParts);
                })
                .toList();
    }

    private AutomaticTranslationWord translate(final AutomaticTranslationWord word, final String languageCode) {
        final String originalWord = word.question();
        final String translatedWord = translationService.translate(originalWord, languageCode, POLISH_LANGUAGE_CODE);

        // Translated word goes to 'question', original word goes to 'answers'
        return new AutomaticTranslationWord(translatedWord, List.of(originalWord));
    }
}
