package com.springboot.manhaji.dto.adaptive;

import lombok.*;

import java.util.List;

/**
 * A single question produced by Gemini.
 * Used internally for serialization into AdaptiveQuizAttempt.generatedQuestionsJson
 * and for server-side answer evaluation.
 * The correctAnswer field is NEVER sent to the client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedQuestion {

    private String type;           // MCQ | TRUE_FALSE | SHORT_ANSWER
    private String questionText;
    private String correctAnswer;  // server-side only
    private List<String> options;  // null for SHORT_ANSWER
    private String subSkill;       // recognition | production | comprehension | application
    private int    difficultyLevel;
}
