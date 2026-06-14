package com.springboot.manhaji.dto.adaptive;

import lombok.*;

import java.util.List;

/** Response body for POST /api/quiz/adaptive/{attemptId}/submit. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdaptiveQuizResult {

    private Long   attemptId;
    private int    score;           // 0–100
    private int    correctCount;
    private int    totalQuestions;
    private int    pointsEarned;    // added to student.totalPoints

    /** Updated skill profiles after this quiz is factored in. */
    private List<SkillSummary> updatedSkills;

    /** Per-question breakdown including correctAnswer and feedback. */
    private List<AdaptiveAnswerFeedback> feedback;
}
