package io.learn.lexigeek.word.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
class MyMemoryTranslationService implements TranslationService {

    private static final String API_URL = "https://api.mymemory.translated.net/get";

    private final RestTemplate restTemplate;

    @Override
    @SuppressWarnings("unchecked")
    public String translate(final String text, final String sourceLanguage, final String targetLanguage) {
        try {
            final String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
            final String langPair = sourceLanguage + "|" + targetLanguage;

            final String url = String.format("%s?q=%s&langpair=%s", API_URL, encodedText, langPair);

            final ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            final Map<String, Object> body = response.getBody();
            if (body == null) {
                return text;
            }

            final Map<String, Object> responseData =
                    (Map<String, Object>) body.get("responseData");

            final String translatedText =
                    responseData != null ? (String) responseData.get("translatedText") : null;

            return translatedText != null ? translatedText : text;

        } catch (final Exception e) {
            log.error("Error translating '{}'", text, e);
            return text;
        }
    }
}
