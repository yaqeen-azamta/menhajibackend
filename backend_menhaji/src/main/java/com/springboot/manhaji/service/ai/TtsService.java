package com.springboot.manhaji.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.manhaji.config.AiConfigProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TtsService {

    private final AiConfigProperties aiConfig;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;  // audit TD3 (2026-04-29)

    private static final String TTS_URL = "https://texttospeech.googleapis.com/v1/text:synthesize";

    public boolean isAvailable() {
        return aiConfig.getGoogleTts().isConfigured();
    }

    /**
     * Synthesize text to speech audio bytes (MP3).
     *
     * @param text     the text to speak
     * @param language "ar" for Arabic, "en" for English
     * @return MP3 audio bytes, or null if unavailable
     */
    public byte[] synthesize(String text, String language) {
        if (!isAvailable()) {
            return null;
        }

        try {
            String voiceName = "ar".equals(language) ? "ar-XA-Wavenet-A" : "en-US-Wavenet-D";
            String languageCode = "ar".equals(language) ? "ar-XA" : "en-US";

            Map<String, Object> requestBody = Map.of(
                    "input", Map.of("text", text),
                    "voice", Map.of(
                            "languageCode", languageCode,
                            "name", voiceName
                    ),
                    "audioConfig", Map.of(
                            "audioEncoding", "MP3",
                            "speakingRate", 0.85,
                            "pitch", 0.0
                    )
            );

            String url = TTS_URL + "?key=" + aiConfig.getGoogleTts().getApiKey();

            String responseJson = webClientBuilder.build()
                    .post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(java.time.Duration.ofSeconds(12));

            return extractAudio(responseJson);
        } catch (Exception e) {
            log.error("TTS synthesis failed: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private byte[] extractAudio(String json) {
        try {
            Map<String, Object> response = objectMapper.readValue(json, Map.class);
            String audioContent = (String) response.get("audioContent");
            return Base64.getDecoder().decode(audioContent);
        } catch (Exception e) {
            log.error("Failed to parse TTS response: {}", e.getMessage());
            return null;
        }
    }
}
