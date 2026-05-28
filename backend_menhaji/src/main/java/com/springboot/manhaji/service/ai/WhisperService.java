package com.springboot.manhaji.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.manhaji.config.AiConfigProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhisperService {

    private final AiConfigProperties aiConfig;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;  // audit TD3 (2026-04-29)

    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta";

    public boolean isAvailable() {
        return aiConfig.getGemini().isConfigured();
    }

    /**
     * Transcribe audio bytes to text using Gemini (free alternative to Whisper).
     *
     * @param audioData the audio file bytes
     * @param language  language code ("ar" for Arabic, "en" for English)
     * @return transcribed text, or an error message if unavailable
     */
    public String transcribe(byte[] audioData, String language) {
        if (!isAvailable()) {
            return "خدمة التعرف على الصوت غير متوفرة حالياً";
        }

        try {
            String base64Audio = Base64.getEncoder().encodeToString(audioData);
            String langName = "ar".equals(language) ? "Arabic" : "English";

            String model = aiConfig.getGemini().getModel();
            String apiKey = aiConfig.getGemini().getApiKey();
            String url = String.format("%s/models/%s:generateContent?key=%s", GEMINI_BASE_URL, model, apiKey);

            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of(
                                            "inlineData", Map.of(
                                                    "mimeType", "audio/webm",
                                                    "data", base64Audio
                                            )
                                    ),
                                    Map.of("text", "Transcribe this audio to " + langName + " text. Return ONLY the transcribed text, nothing else.")
                            ))
                    ),
                    "generationConfig", Map.of(
                            "temperature", 0.1,
                            "maxOutputTokens", 512
                    )
            );

            String responseJson = webClientBuilder.build()
                    .post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(java.time.Duration.ofSeconds(12));

            return extractTextFromGeminiResponse(responseJson);
        } catch (Exception e) {
            log.error("Audio transcription failed: {}", e.getMessage());
            return "حدث خطأ في التعرف على الصوت. حاول مرة أخرى.";
        }
    }

    private String extractTextFromGeminiResponse(String json) {
        try {
            var root = objectMapper.readTree(json);
            return root.path("candidates").path(0)
                    .path("content").path("parts").path(0)
                    .path("text").asText();
        } catch (Exception e) {
            log.error("Failed to parse transcription response: {}", e.getMessage());
            return "حدث خطأ في معالجة الصوت";
        }
    }
}
