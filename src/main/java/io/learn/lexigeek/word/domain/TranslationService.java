package io.learn.lexigeek.word.domain;

interface TranslationService {
    String translate(final String text, final String sourceLanguage, final String targetLanguage);
}
