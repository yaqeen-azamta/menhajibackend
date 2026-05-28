package com.springboot.manhaji.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TracingProgressResponse {

    private Long studentId;

    /** Total tracing attempts across all characters. */
    private int totalAttempts;

    /** Number of attempts that resulted in isCorrect = true. */
    private int totalCorrect;

    /** Average finalAccuracy across all attempts (0-100). */
    private double overallAccuracy;

    /** Total stars accumulated across all attempts. */
    private int totalStars;

    /** Maximum possible stars = 3 × totalAttempts. */
    private int maxPossibleStars;

    /** Percentage of characters the student has mastered (bestAccuracy >= 80). */
    private double masteryRate;

    /** Per-character breakdown. */
    private List<CharacterProgress> characterProgress;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CharacterProgress {
        private Long questionId;
        private String character;
        private String displayName;
        private int attempts;
        private int bestStars;
        private double bestAccuracy;
        private double latestAccuracy;
        /** true when bestAccuracy >= 80%. */
        private boolean mastered;
    }
}
