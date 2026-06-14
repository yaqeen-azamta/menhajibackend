package com.springboot.manhaji.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.manhaji.config.AiConfigProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.springboot.manhaji.exception.SpeechServiceUnavailableException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Transcribes a child's reading audio against an expected lesson text using Gemini.
 * Named GoogleSpeechService to align with the planned migration to Google Cloud STT;
 * the underlying transport is Gemini (same provider already used by WhisperService and TtsService).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleSpeechService {

    private final AiConfigProperties aiConfig;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta";
    private static final int    MAX_RETRIES     = 3;
    private static final long[] BACKOFF_MS      = {2_000L, 4_000L, 8_000L}; // exponential backoff
    private static final String FALLBACK_MODEL  = "gemini-2.0-flash";        // used when primary is overloaded

    public boolean isAvailable() {
        return aiConfig.getGemini().isConfigured();
    }

    /**
     * Transcribes {@code audioFile} with the context of {@code lessonText} so Gemini can
     * produce a more accurate transcript for Arabic/English Grade-1 reading exercises.
     *
     * @param audioFile  multipart audio from Flutter (webm, m4a, wav, mp3, ogg)
     * @param lessonText the original paragraph the student is supposed to read
     * @param language   "ar" or "en"
     * @return raw transcribed text, or empty string on failure
     */
    public String transcribeForReading(MultipartFile audioFile, String lessonText, String language) throws Exception {
        System.out.println("[READING] GoogleSpeechService.transcribeForReading CALLED");

        byte[] audioData = audioFile.getBytes();

        // ── Audio pipeline diagnostics ──────────────────────────────────────────────
        String originalFilename    = audioFile.getOriginalFilename();
        String reportedContentType = audioFile.getContentType();   // what Flutter claims
        String mimeType            = resolveMimeType(audioFile);   // what we send to Gemini
        String detectedFormat      = detectAudioFormat(audioData); // from magic bytes

        System.out.println("============================================================");
        System.out.println("[AUDIO] originalFilename  = " + originalFilename);
        System.out.println("[AUDIO] reportedMimeType  = " + reportedContentType);
        System.out.println("[AUDIO] resolvedMimeType  = " + mimeType + "  <-- sent to Gemini");
        System.out.println("[AUDIO] detectedFormat    = " + detectedFormat + "  <-- from magic bytes");
        System.out.println("[AUDIO] fileSize          = " + audioData.length + " bytes");
        System.out.println("[AUDIO] first16Bytes      = " + toHex(audioData, 16));
        System.out.println("============================================================");

        log.info("[READING] AUDIO: filename={} reportedType={} resolvedType={} detected={} size={}B first16={}",
                originalFilename, reportedContentType, mimeType, detectedFormat,
                audioData.length, toHex(audioData, 16));

        // Save raw audio bytes for manual playback / inspection
        try {
            java.nio.file.Path debugPath = java.nio.file.Path.of("debug-reading-audio.bin");
            java.nio.file.Files.write(debugPath, audioData);
            System.out.println("[AUDIO] saved to: " + debugPath.toAbsolutePath());
            log.info("[READING] AUDIO: debug file saved → {}", debugPath.toAbsolutePath());
        } catch (Exception saveEx) {
            log.warn("[READING] AUDIO: could not save debug file — {}", saveEx.getMessage());
        }

        String base64Audio = Base64.getEncoder().encodeToString(audioData);
        String langName = "ar".equals(language) ? "Arabic" : "English";

        String model = aiConfig.getGemini().getModel();
        String apiKey = aiConfig.getGemini().getApiKey();

        boolean keySet = apiKey != null && !apiKey.isBlank() && !"not-set".equals(apiKey);
        String keyHint = keySet ? (apiKey.substring(0, Math.min(6, apiKey.length())) + "***") : "NOT_SET";
        System.out.println("[READING] Gemini key present=" + keySet + " hint=" + keyHint + " model=" + model);
        log.info("[READING] transcribeForReading: model={} language={} apiKeyPresent={} apiKeyHint={}",
                model, language, keySet, keyHint);

        String prompt = String.format(
                "You are a speech-to-text engine. " +
                "The speaker is a child reading aloud in %s. " +
                "Listen carefully to the audio and transcribe exactly what you hear. " +
                "Return ONLY the spoken words with no explanation, no corrections, no commentary.",
                langName);

        log.info("[READING] transcribeForReading: prompt summary — lang={} promptLen={}",
                langName, prompt.length());

        // ── Primary model with exponential backoff, then fallback on service failure ──
        String responseJson;
        String engineUsed = model;

        try {
            log.info("[READING] trying primary model: {}", model);
            System.out.println("[READING] PRIMARY MODEL: " + model);
            responseJson = attemptTranscription(model, apiKey, base64Audio, mimeType, prompt);
            log.info("[READING] primary model succeeded: {}", model);
        } catch (SpeechServiceUnavailableException primaryEx) {
            // Service-level failure (503/429) exhausted all retries — try fallback model
            log.warn("[READING] primary model {} failed after {} retries — falling back to {}",
                    model, MAX_RETRIES, FALLBACK_MODEL, primaryEx);
            System.out.println("[READING] PRIMARY FAILED (" + model + ") — trying fallback: " + FALLBACK_MODEL);
            try {
                responseJson = attemptTranscription(FALLBACK_MODEL, apiKey, base64Audio, mimeType, prompt);
                engineUsed = FALLBACK_MODEL;
                log.info("[READING] fallback model succeeded: {}", FALLBACK_MODEL);
                System.out.println("[READING] FALLBACK SUCCEEDED: " + FALLBACK_MODEL);
            } catch (Exception fallbackEx) {
                log.error("[READING] fallback model {} also failed — both models unavailable",
                        FALLBACK_MODEL, fallbackEx);
                System.out.println("[READING] FALLBACK ALSO FAILED: " + FALLBACK_MODEL);
                throw new SpeechServiceUnavailableException(
                        "Primary (" + model + ") and fallback (" + FALLBACK_MODEL + ") both unavailable", fallbackEx);
            }
        }

        log.info("[READING] transcribeForReading: engine={} raw response = {}", engineUsed, responseJson);
        System.out.println("[READING] engine=" + engineUsed + " raw response: " + responseJson);

        String extracted = extractText(responseJson);
        System.out.println("=================================");
System.out.println("EXTRACTED TEXT = [" + extracted + "]");
System.out.println("ENGINE USED    = " + engineUsed);
System.out.println("=================================");
        if (extracted.isBlank()) {
            log.error("[READING] transcribeForReading: extracted text is EMPTY — engine={} raw response was: {}",
                    engineUsed, responseJson);
            System.out.println("[READING] extracted text is EMPTY");
        } else {
            log.info("[READING] transcribeForReading: engine={} extracted = \"{}\"", engineUsed, extracted);
            System.out.println("[READING] extracted text: \"" + extracted + "\"");
        }
        return extracted;
    }

    /**
     * Calls the Gemini API for the given {@code model} with exponential-backoff retry.
     * Returns the raw JSON response string on success.
     * Throws {@link SpeechServiceUnavailableException} when all retries are exhausted with
     * a retryable error (503, 429, network/timeout).
     * Re-throws non-retryable errors (400, 401, etc.) immediately without retry.
     */
    private String attemptTranscription(String model, String apiKey,
                                         String base64Audio, String mimeType,
                                         String prompt) throws Exception {
        String url = String.format("%s/models/%s:generateContent?key=%s", GEMINI_BASE_URL, model, apiKey);
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("inlineData", Map.of("mimeType", mimeType, "data", base64Audio)),
                                Map.of("text", prompt)
                        ))
                ),
                "generationConfig", Map.of("temperature", 0.1, "maxOutputTokens", 512)
        );

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                String responseJson = webClientBuilder.build()
                        .post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block(java.time.Duration.ofSeconds(30));
                log.info("[READING] attemptTranscription: model={} attempt={}/{} succeeded", model, attempt, MAX_RETRIES);
                return responseJson;
            } catch (Exception e) {
                if (e instanceof WebClientResponseException wcre) {
                    log.error("[READING] attemptTranscription: model={} attempt={}/{} HTTP error — status={} body={}",
                            model, attempt, MAX_RETRIES, wcre.getStatusCode(), wcre.getResponseBodyAsString());
                    System.out.println("[READING] " + model + " attempt=" + attempt
                            + " HTTP error status=" + wcre.getStatusCode()
                            + " body=" + wcre.getResponseBodyAsString());
                } else {
                    log.error("[READING] attemptTranscription: model={} attempt={}/{} error — {}: {}",
                            model, attempt, MAX_RETRIES, e.getClass().getSimpleName(), e.getMessage());
                    System.out.println("[READING] " + model + " attempt=" + attempt
                            + " error " + e.getClass().getSimpleName() + ": " + e.getMessage());
                }

                if (!isRetryable(e)) {
                    throw e; // non-retryable (400, 401, etc.) — fail immediately, no fallback
                }

                if (attempt == MAX_RETRIES) {
                    log.error("[READING] attemptTranscription: model={} all {} retries exhausted",
                            model, MAX_RETRIES, e);
                    System.out.println("[READING] " + model + " all " + MAX_RETRIES + " retries exhausted");
                    throw new SpeechServiceUnavailableException(
                            "Model " + model + " temporarily unavailable after " + MAX_RETRIES + " attempts", e);
                }

                long delay = BACKOFF_MS[attempt - 1];
                log.warn("[READING] attemptTranscription: model={} attempt={}/{} failed — retrying in {}ms",
                        model, attempt, MAX_RETRIES, delay);
                System.out.println("[READING] " + model + " retrying in " + delay + "ms"
                        + " (attempt " + attempt + "/" + MAX_RETRIES + ")");
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new SpeechServiceUnavailableException(
                            "Interrupted while retrying " + model, ie);
                }
            }
        }
        // unreachable — loop always returns or throws above
        throw new SpeechServiceUnavailableException("Model " + model + " failed", null);
    }

    private boolean isRetryable(Exception e) {
        if (e instanceof WebClientResponseException wcre) {
            int status = wcre.getStatusCode().value();
            return status == 429 || status == 503;
        }
        if (e instanceof WebClientRequestException) {
            return true; // connection refused, read timeout, etc.
        }
        if (e instanceof java.io.IOException) {
            return true;
        }
        // .block(Duration) timeout throws IllegalStateException wrapping TimeoutException
        if (e instanceof IllegalStateException
                && e.getMessage() != null
                && e.getMessage().contains("Timeout on blocking read")) {
            return true;
        }
        return false;
    }

    private String detectAudioFormat(byte[] data) {
        if (data == null || data.length < 4) return "UNKNOWN (file too small: " + (data == null ? 0 : data.length) + " bytes)";
        int b0 = data[0] & 0xFF;
        int b1 = data[1] & 0xFF;
        int b2 = data[2] & 0xFF;
        int b3 = data[3] & 0xFF;
        // WebM / Matroska
        if (b0 == 0x1A && b1 == 0x45 && b2 == 0xDF && b3 == 0xA3) return "audio/webm (WebM/Matroska)";
        // RIFF WAV
        if (b0 == 0x52 && b1 == 0x49 && b2 == 0x46 && b3 == 0x46) return "audio/wav (RIFF/WAV)";
        // OGG
        if (b0 == 0x4F && b1 == 0x67 && b2 == 0x67 && b3 == 0x53) return "audio/ogg (OGG)";
        // MP3 with ID3 tag
        if (b0 == 0x49 && b1 == 0x44 && b2 == 0x33) return "audio/mpeg (MP3 with ID3 tag)";
        // MP3 sync word (0xFFEx or 0xFFFx)
        if (b0 == 0xFF && (b1 & 0xE0) == 0xE0) return "audio/mpeg (MP3 sync)";
        // AAC ADTS (0xFFF1 = MPEG4 AAC, 0xFFF9 = MPEG2 AAC)
        if (b0 == 0xFF && (b1 == 0xF1 || b1 == 0xF9)) return "audio/aac (AAC ADTS)";
        // MP4 / M4A — bytes 4-7 are "ftyp"
        if (data.length >= 8) {
            int b4 = data[4] & 0xFF, b5 = data[5] & 0xFF, b6 = data[6] & 0xFF, b7 = data[7] & 0xFF;
            if (b4 == 0x66 && b5 == 0x74 && b6 == 0x79 && b7 == 0x70) return "audio/mp4 (MP4/M4A — ftyp box)";
        }
        return String.format("UNKNOWN (magic: %02X %02X %02X %02X)", b0, b1, b2, b3);
    }

    private String toHex(byte[] data, int maxBytes) {
        if (data == null || data.length == 0) return "(empty)";
        int len = Math.min(data.length, maxBytes);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            if (i > 0) sb.append(' ');
            sb.append(String.format("%02X", data[i] & 0xFF));
        }
        if (data.length > maxBytes) sb.append(" ...");
        return sb.toString();
    }

    private String resolveMimeType(MultipartFile file) {
        String ct = file.getContentType();
        if (ct != null && !ct.isBlank() && !"application/octet-stream".equals(ct)) {
            return ct;
        }
        String name = file.getOriginalFilename();
        if (name != null) {
            String lower = name.toLowerCase();
            if (lower.endsWith(".webm")) return "audio/webm";
            if (lower.endsWith(".m4a") || lower.endsWith(".aac")) return "audio/mp4";
            if (lower.endsWith(".wav")) return "audio/wav";
            if (lower.endsWith(".mp3")) return "audio/mpeg";
            if (lower.endsWith(".ogg")) return "audio/ogg";
        }
        return "audio/webm";
    }

    private String extractText(String json) {
        System.out.println("JSON RECEIVED:");
System.out.println(json);
        if (json == null || json.isBlank()) {
            log.error("[READING] extractText: input JSON is null/blank");
            System.out.println("[READING] extractText: input JSON is null/blank");
            return "";
        }
        try {
            var root = objectMapper.readTree(json);
            // Log top-level keys to diagnose unexpected response shapes (e.g. error objects)
            log.info("[READING] extractText: top-level keys = {}", root.fieldNames());
            String text = root.path("candidates").path(0)
                    .path("content").path("parts").path(0)
                    .path("text").asText("").trim();
            return text;
        } catch (Exception e) {
            log.error("[READING] extractText: FAILED to parse Gemini transcription response: {} — json={}",
                    e.getMessage(), json, e);
            System.out.println("[READING] extractText PARSE ERROR: " + e.getMessage() + " json=" + json);
            return "";
        }
    }
}
