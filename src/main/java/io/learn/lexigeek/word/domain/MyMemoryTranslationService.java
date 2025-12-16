package io.learn.lexigeek.word.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Implementation using MyMemory Translation API (free, no API key required)
 * API documentation: <a href="https://mymemory.translated.net/doc/spec.php">MyMemory API Docs</a>
 */
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
            // Encode the text for URL
            String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String langPair = sourceLanguage + "|" + targetLanguage;

            // Build the full URL
            String url = String.format("%s?q=%s&langpair=%s", API_URL, encodedText, langPair);

            log.debug("Translating '{}' from {} to {}", text, sourceLanguage, targetLanguage);
            // Make the API call
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();

                // Extract translatedText from responseData
                if (body.containsKey("responseData")) {
                    Map<String, Object> responseData = (Map<String, Object>) body.get("responseData");
                    if (responseData != null && responseData.containsKey("translatedText")) {
                        String translatedText = (String) responseData.get("translatedText");
                        log.debug("Translation result: '{}'", translatedText);
                        return translatedText;
                    }
                }
            }

            log.warn("Failed to translate '{}': Invalid response", text);
            return text; // Return original text if translation fails

        } catch (Exception e) {
            log.error("Error translating text '{}': {}", text, e.getMessage(), e);
            return text; // Return original text if translation fails
        }
    }
}
