package com.springboot.manhaji.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.manhaji.config.AiConfigProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Text-to-speech service.
 *
 * <p>Supports two providers, switchable via {@code app.ai.tts-provider}:
 * <ul>
 *   <li><b>edge</b> (default, recommended): Microsoft Edge's free neural
 *       TTS, accessed via the {@code edge-tts} Python library. Same voice
 *       quality you'd pay Azure for — no API key, no signup, no credit
 *       card.</li>
 *   <li><b>google</b>: Google Cloud TTS, requires {@code GOOGLE_TTS_API_KEY}
 *       and a billing-enabled GCP project. Kept as a fallback path.</li>
 * </ul>
 *
 * <p>Audio is cached upstream on {@code Question.audioUrl} /
 * {@code Lesson.audioUrl} — see {@code AudioController}. This service only
 * synthesizes; it doesn't store the bytes. The cache key is owned here:
 * {@link #speechFingerprint} returns a hash of the spoken text that the
 * controller persists alongside the URL so the cache self-invalidates when
 * content changes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TtsService {

    private final AiConfigProperties aiConfig;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    private static final String GOOGLE_TTS_URL =
            "https://texttospeech.googleapis.com/v1/text:synthesize";

    /**
     * The Edge TTS sidecar Python script is shipped inside the jar as a
     * classpath resource. To exec it, we copy it to a real file on disk
     * once at startup and remember the path.
     */
    private Path edgeSidecarPath;

    @PostConstruct
    void extractSidecarOnce() {
        if (!"edge".equalsIgnoreCase(aiConfig.getTtsProvider())) return;
        try {
            edgeSidecarPath = extractClasspathScript("tts/edge_tts_sidecar.py");
            log.info("Edge TTS sidecar extracted to {}", edgeSidecarPath);
        } catch (IOException e) {
            log.error("Failed to extract Edge TTS sidecar script — TTS will fall back: {}",
                    e.getMessage());
        }
    }

    /**
     * True iff the active provider is wired up. Callers use this to decide
     * whether to invoke {@link #synthesize} or skip to the client-side fallback.
     */
    public boolean isAvailable() {
        String provider = aiConfig.getTtsProvider();
        if ("edge".equalsIgnoreCase(provider)) {
            return aiConfig.getEdgeTts().isConfigured() && edgeSidecarPath != null;
        }
        return aiConfig.getGoogleTts().isConfigured();
    }

    /**
     * Synthesize a chunk of text to MP3 bytes.
     *
     * @param text     UTF-8 text. Arabic, English, or mixed.
     * @param language {@code "ar"} or {@code "en"}.
     * @return MP3 bytes, or {@code null} on any failure.
     */
    public byte[] synthesize(String text, String language) {
        if (text == null || text.isBlank()) return null;
        if (!isAvailable()) return null;

        text = sanitizeForSpeech(text);
        if (text.isBlank()) return null;

        String provider = aiConfig.getTtsProvider();
        if ("edge".equalsIgnoreCase(provider)) {
            return synthesizeViaEdge(text, language);
        }
        return synthesizeViaGoogle(text, language);
    }

    private static final String BLANK_SPOKEN = " … ";

    /**
     * Strips non-speakable markup from question/lesson text before it goes to
     * the voice. Collapses any run of underscores (the FILL_BLANK "___" marker)
     * into a spoken pause.
     */
    private static String sanitizeForSpeech(String text) {
        if (text == null) return "";
        String out = text.replaceAll("_{2,}", BLANK_SPOKEN);
        out = out.replaceAll("[ \\t]{2,}", " ").trim();
        return out;
    }

    /**
     * A stable fingerprint of the audio that {@link #synthesize} would produce
     * for {@code text} — a SHA-256 of the sanitized spoken form.
     *
     * <p>Callers persist this next to the cached audio URL and compare it on
     * the next read: if the source text has changed, the fingerprints differ
     * and the audio regenerates automatically.
     *
     * @return hex SHA-256 string, or {@code null} for null/blank input.
     */
    public String speechFingerprint(String text) {
        if (text == null) return null;
        String spoken = sanitizeForSpeech(text);
        if (spoken.isBlank()) return null;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(spoken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            log.warn("SHA-256 unavailable, TTS cache cannot self-invalidate", e);
            return null;
        }
    }

    // ============================================================
    // Edge TTS (free, no key)
    // ============================================================

    /**
     * Execs the Python sidecar to synthesize via Microsoft Edge's neural TTS.
     * Text is written on the process's stdin as raw UTF-8 bytes to avoid
     * command-line argv encoding issues with Arabic on Windows.
     */
    private byte[] synthesizeViaEdge(String text, String language) {
        if (edgeSidecarPath == null) {
            log.warn("Edge TTS sidecar not extracted — cannot synthesize");
            return null;
        }
        AiConfigProperties.EdgeTts cfg = aiConfig.getEdgeTts();
        String voice = "en".equalsIgnoreCase(language)
                ? cfg.getVoiceEnglish()
                : cfg.getVoiceArabic();

        String safeText = stripLoneSurrogates(text);

        Path mp3Out = null;
        try {
            mp3Out = Files.createTempFile("manhaji-tts-", ".mp3");
            ProcessBuilder pb = new ProcessBuilder(
                    cfg.getPythonPath(),
                    edgeSidecarPath.toString(),
                    voice,
                    mp3Out.toString());
            pb.redirectErrorStream(false);
            Process process = pb.start();

            try (var stdin = process.getOutputStream()) {
                stdin.write(safeText.getBytes(StandardCharsets.UTF_8));
                stdin.flush();
            }

            boolean finished = process.waitFor(cfg.getTimeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("Edge TTS sidecar timed out after {}s for voice={}",
                        cfg.getTimeoutSeconds(), voice);
                return null;
            }
            if (process.exitValue() != 0) {
                String stderr = new String(
                        process.getErrorStream().readAllBytes(),
                        StandardCharsets.UTF_8);
                log.error("Edge TTS sidecar exit {} for voice={}: {}",
                        process.exitValue(), voice, stderr.trim());
                return null;
            }
            byte[] mp3 = Files.readAllBytes(mp3Out);
            if (mp3.length == 0) {
                log.warn("Edge TTS produced empty mp3 for voice={}", voice);
                return null;
            }
            return mp3;
        } catch (Exception e) {
            log.error("Edge TTS synthesis failed for voice={}: {}", voice, e.getMessage());
            return null;
        } finally {
            if (mp3Out != null) {
                try { Files.deleteIfExists(mp3Out); } catch (IOException ignored) {}
            }
        }
    }

    /**
     * Removes any unpaired UTF-16 surrogate code units from {@code text}.
     * A no-op for healthy inputs; exists to defang pathological strings from
     * corrupt curriculum data.
     */
    private static String stripLoneSurrogates(String text) {
        if (text == null || text.isEmpty()) return text;
        StringBuilder out = null;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean isHigh = Character.isHighSurrogate(c);
            boolean isLow = Character.isLowSurrogate(c);
            boolean paired = false;
            if (isHigh && i + 1 < text.length() && Character.isLowSurrogate(text.charAt(i + 1))) {
                paired = true;
            } else if (isLow && i > 0 && Character.isHighSurrogate(text.charAt(i - 1))) {
                paired = true;
            }
            if ((isHigh || isLow) && !paired) {
                if (out == null) {
                    out = new StringBuilder(text.length());
                    out.append(text, 0, i);
                }
                continue;
            }
            if (out != null) out.append(c);
        }
        return out == null ? text : out.toString();
    }

    /**
     * Copies a classpath resource (the Python sidecar) to a temp file on disk
     * so ProcessBuilder can exec it.
     */
    private Path extractClasspathScript(String resourcePath) throws IOException {
        ClassPathResource res = new ClassPathResource(resourcePath);
        if (!res.exists()) {
            throw new IOException("Classpath resource not found: " + resourcePath);
        }
        Path target = Files.createTempFile("manhaji-tts-", ".py");
        try (var in = res.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        target.toFile().setExecutable(true);
        return target;
    }

    // ============================================================
    // Google Cloud TTS (legacy fallback)
    // ============================================================

    private byte[] synthesizeViaGoogle(String text, String language) {
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

            String url = GOOGLE_TTS_URL + "?key=" + aiConfig.getGoogleTts().getApiKey();

            String responseJson = webClientBuilder.build()
                    .post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(12));

            return extractGoogleAudio(responseJson);
        } catch (Exception e) {
            log.error("Google TTS synthesis failed: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private byte[] extractGoogleAudio(String json) {
        try {
            Map<String, Object> response = objectMapper.readValue(json, Map.class);
            String audioContent = (String) response.get("audioContent");
            return Base64.getDecoder().decode(audioContent);
        } catch (Exception e) {
            log.error("Failed to parse Google TTS response: {}", e.getMessage());
            return null;
        }
    }
}
