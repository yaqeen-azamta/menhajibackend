package com.springboot.manhaji.dto.adaptive;

import lombok.*;

import java.util.List;

/** Response body for GET /api/quiz/adaptive/{lessonId}. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdaptiveQuizPayload {

    private Long   attemptId;
    private Long   lessonId;
    private String lessonTitle;

    /** Difficulty level (1–5) used when generating this quiz. */
    private int difficulty;

    /** Sub-skill names identified as weak and targeted in this quiz. */
    private List<String> focusSkills;

    private int questionCount;
    private List<AdaptiveQuizItem> questions;

    /** "GEMINI" when AI-generated, "FALLBACK_DB" when sourced from the question bank. */
    private String source;
}
