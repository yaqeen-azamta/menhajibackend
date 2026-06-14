package com.springboot.manhaji.dto.adaptive;

import lombok.*;

import java.util.List;

/** All context passed to GeminiService when building the adaptive quiz prompt. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdaptiveQuizContext {

    private String lessonTitle;
    private String subjectName;
    private String lessonContent;     // truncated before sending to Gemini
    private String lessonObjectives;

    /** Computed target difficulty (1–5) for this session. */
    private int targetDifficulty;

    /** Total questions to request from Gemini. */
    private int questionCount;

    /** Sub-skills where accuracy < threshold → 60% of questions come from here. */
    private List<SkillSummary> weakSkills;

    /** Sub-skills where accuracy >= threshold → 40% for review. */
    private List<SkillSummary> strongSkills;
}
