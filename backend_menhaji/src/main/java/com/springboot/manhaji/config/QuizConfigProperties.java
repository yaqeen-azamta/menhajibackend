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
}
