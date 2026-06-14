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
        String rawKey = gemini.getApiKey();
        boolean keyPresent = rawKey != null && !rawKey.isBlank();
        boolean isPlaceholder = keyPresent && (rawKey.equals("not-set") || rawKey.startsWith("REPLACE_"));
        boolean configured = gemini.isConfigured();

        log.info("════════════════════════════════════════════════");
        log.info("  AI CONFIGURATION REPORT");
        log.info("════════════════════════════════════════════════");

        if (configured) {
            String masked = rawKey.substring(0, Math.min(6, rawKey.length())) + "***";
            log.info("  Gemini AI   : ENABLED");
            log.info("  API key     : detected (prefix: {})", masked);
            log.info("  Model       : {}", gemini.getModel());
            if (!rawKey.startsWith("AIzaSy")) {
                log.warn("  ⚠ Key format warning: Gemini AI Studio keys start with 'AIzaSy'.");
                log.warn("    Current prefix: '{}'. If API calls fail with 401/403, get a", masked);
                log.warn("    new key from https://aistudio.google.com/apikey");
            }
        } else if (isPlaceholder) {
            log.error("  Gemini AI   : DISABLED — API key is still the placeholder value");
            log.error("  API key     : \"{}\" (this is NOT a real key)", rawKey);
            log.error("  HOW TO FIX: set app.ai.gemini.api-key in application.yaml");
            log.error("  Get a free key at: https://aistudio.google.com/apikey");
        } else {
            log.warn("  Gemini AI   : DISABLED — API key not set or not recognised");
            log.warn("  API key raw : \"{}\"", rawKey);
            log.warn("  HOW TO FIX: set app.ai.gemini.api-key in application.yaml");
            log.warn("  Get a free key at: https://aistudio.google.com/apikey");
            log.warn("  Impact: Adaptive quizzes use DB fallback. Hints use static text.");
        }

        if (googleTts.isConfigured()) {
            log.info("  Google TTS  : ENABLED");
        } else {
            log.info("  Google TTS  : using Edge TTS fallback (no GOOGLE_TTS_API_KEY set)");
        }
        log.info("════════════════════════════════════════════════");
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
            return apiKey != null
                && !apiKey.isBlank()
                && !"not-set".equals(apiKey)
                && !apiKey.startsWith("REPLACE_")       // catches REPLACE_WITH_YOUR_GEMINI_API_KEY
                && !apiKey.equals("your_actual_key_here"); // guard against literal copy-paste from docs
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
