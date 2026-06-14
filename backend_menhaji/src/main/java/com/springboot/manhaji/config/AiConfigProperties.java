package com.springboot.manhaji.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.ai")
@Getter
@Setter
@Slf4j
public class AiConfigProperties {

    private Gemini gemini = new Gemini();
    private Whisper whisper = new Whisper();
    private GoogleTts googleTts = new GoogleTts();
    private EdgeTts edgeTts = new EdgeTts();

    @PostConstruct
    public void logConfigurationStatus() {
        if (gemini.isConfigured()) {
            String hint = gemini.getApiKey().substring(0, Math.min(6, gemini.getApiKey().length()));
            log.info("Gemini AI  : ENABLED  (key prefix: {}***,  model: {})", hint, gemini.getModel());
        } else {
            log.warn("Gemini AI  : DISABLED — set GEMINI_API_KEY environment variable to enable adaptive quizzes and hints");
        }

        if (googleTts.isConfigured()) {
            log.info("Google TTS : ENABLED");
        } else {
            log.info("Google TTS : using Edge TTS fallback (no GOOGLE_TTS_API_KEY)");
        }
    }
    /**
     * Which TTS provider {@link com.springboot.manhaji.service.ai.TtsService}
     * uses. Values: {@code "edge"} (Microsoft Edge neural voices, free, no
     * key — preferred) or {@code "google"} (Google Cloud TTS, requires
     * billing-enabled GOOGLE_TTS_API_KEY).
     */
    private String ttsProvider = "edge";

    @Getter
    @Setter
    public static class Gemini {
        private String apiKey = "not-set";
        private String model = "gemini-2.5-flash";

        public boolean isConfigured() {
            return apiKey != null && !apiKey.isBlank() && !"not-set".equals(apiKey);
        }
    }

    @Getter
    @Setter
    public static class Whisper {
        private String apiKey = "not-set";

        public boolean isConfigured() {
            return apiKey != null && !apiKey.isBlank() && !"not-set".equals(apiKey);
        }
    }

    @Getter
    @Setter
    public static class GoogleTts {
        private String apiKey = "not-set";

        public boolean isConfigured() {
            return apiKey != null && !apiKey.isBlank() && !"not-set".equals(apiKey);
        }
    }

    /**
     * Microsoft Edge TTS — free neural voices, no API key. Wraps the
     * {@code edge-tts} Python library via a sidecar script. See
     * {@code src/main/resources/tts/edge_tts_sidecar.py} for the runtime
     * and {@code src/main/resources/tts/requirements.txt} for install.
     */
    @Getter
    @Setter
    public static class EdgeTts {
        /** Path to the Python interpreter. Use {@code python} if it's on PATH. */
        private String pythonPath = "python";

        /** Voice for Arabic questions. Levantine female (Jordanian) by default. */
        private String voiceArabic = "ar-JO-SanaNeural";

        /** Voice for English questions. Microsoft's flagship warm-teacher voice. */
        private String voiceEnglish = "en-US-AriaNeural";

        /** Max wall-clock seconds for a single synthesize call. */
        private int timeoutSeconds = 20;

        /** Edge TTS needs no API key, so this is always "configured". */
        public boolean isConfigured() {
            return true;
        }
    }
}
