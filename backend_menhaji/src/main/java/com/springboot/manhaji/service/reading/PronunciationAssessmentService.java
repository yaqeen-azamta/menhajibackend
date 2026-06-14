package com.springboot.manhaji.service.reading;

import java.util.List;

/**
 * Extension point for phoneme-level and word-level pronunciation scoring.
 *
 * <p><b>Phase 1:</b> interface only — no implementation bean is registered.
 * {@link com.springboot.manhaji.service.reading.ReadingService} injects this with
 * {@code @Autowired(required = false)}, so Spring will not fail to start when no
 * implementation is present.
 *
 * <p><b>Phase 2:</b> register a concrete {@code @Service} that implements this interface
 * and it will be picked up automatically with zero changes to the controller or service layer.
 * Candidate implementations:
 * <ul>
 *   <li>{@code WhisperPronunciationService} — OpenAI Whisper word-timestamp API</li>
 *   <li>{@code AzureCognitiveSpeechService} — Azure Pronunciation Assessment API</li>
 *   <li>{@code GeminiPronunciationService} — Gemini audio analysis with phoneme prompts</li>
 * </ul>
 *
 * <p><b>Contract for all implementations:</b>
 * <ul>
 *   <li>Must not block longer than 30 seconds.</li>
 *   <li>Must never throw — return {@link PronunciationDetail#empty()} on any failure.</li>
 *   <li>Must be thread-safe (Spring prototype/singleton scope).</li>
 * </ul>
 */
public interface PronunciationAssessmentService {

    /**
     * Evaluates pronunciation quality against the expected text.
     *
     * @param audioData      raw audio bytes from Flutter (any format supported by the implementation)
     * @param expectedText   the original lesson paragraph the student was asked to read
     * @param recognizedText the transcript already produced by the STT pipeline
     * @param language       "ar" for Arabic, "en" for English
     * @return per-word pronunciation scores; never null — return {@link PronunciationDetail#empty()} if unavailable
     */
    PronunciationDetail assess(
            byte[] audioData,
            String expectedText,
            String recognizedText,
            String language
    );

    /**
     * @return {@code false} when this implementation's external dependency (API key,
     *         service endpoint, model) is not configured. The service layer will skip
     *         pronunciation scoring gracefully.
     */
    boolean isAvailable();

    // ─────────────────────────────────────────────────────────────────────────────
    //  Result types — extended in Phase 2 with phoneme detail, confidence, timing
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Top-level pronunciation result for one reading attempt.
     *
     * @param overallScore 0.0–100.0 pronunciation quality across the whole text
     * @param wordScores   per-word breakdown; empty list when not available
     */
    record PronunciationDetail(
            double overallScore,
            List<WordScore> wordScores
    ) {
        /** Safe no-op result used when the service is unavailable or fails. */
        public static PronunciationDetail empty() {
            return new PronunciationDetail(0.0, List.of());
        }
    }

    /**
     * Pronunciation score for a single word.
     *
     * @param word     the expected word from the lesson text
     * @param score    0.0–100.0 pronunciation accuracy for this word
     * @param feedback short child-facing feedback in Arabic (e.g. "ممتاز", "حاول مرة أخرى")
     */
    record WordScore(
            String word,
            double score,
            String feedback
    ) {}
}
