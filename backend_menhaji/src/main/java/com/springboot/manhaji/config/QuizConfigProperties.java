package com.springboot.manhaji.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Tunable quiz scoring / progression constants. Previously inlined in
 * {@code QuizService} and difficult to adjust without a rebuild.
 */
@Configuration
@ConfigurationProperties(prefix = "app.quiz")
@Getter
@Setter
public class QuizConfigProperties {

    /** Points awarded per correct answer. */
    private int pointsPerCorrect = 10;

    /** Lesson score (0-100) at which progress is marked MASTERED. */
    private double masteryThreshold = 80.0;

    /** Lesson score (0-100) at which progress is marked COMPLETED. */
    private double completionThreshold = 50.0;

    /** Maximum hint level the student can request for a question. */
    private int maxHintLevel = 3;

    // --- Adaptive quiz settings ---

    /** Number of questions generated per adaptive quiz session. */
    private int adaptiveQuestionCount = 10;

    /** Accuracy % below which a sub-skill is considered weak (gets 60% question weight). */
    private double adaptiveWeakSkillThreshold = 60.0;

    // --- Hint rate limits (persisted to DB, enforced server-side) ---

    /** Maximum hint requests allowed for a single question within one attempt. */
    private int maxHintsPerQuestion = 3;

    /** Maximum total hint requests across all questions in one attempt. */
    private int maxHintsPerAttempt = 10;

    /**
     * When true and Gemini fails after one retry, the service falls back to
     * existing database questions for the lesson rather than returning an error.
     */
    private boolean adaptiveFallbackEnabled = true;
}
