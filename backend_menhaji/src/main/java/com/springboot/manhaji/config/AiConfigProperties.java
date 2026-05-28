package com.springboot.manhaji.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.ai")
@Getter
@Setter
public class AiConfigProperties {

    private Gemini gemini = new Gemini();
    private Whisper whisper = new Whisper();
    private GoogleTts googleTts = new GoogleTts();

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
}
