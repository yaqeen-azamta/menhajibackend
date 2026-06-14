package com.springboot.manhaji.dto.adaptive;

import lombok.*;

/** Per-question feedback included in AdaptiveQuizResult. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdaptiveAnswerFeedback {

    private int     questionIndex;
    private String  questionText;
    private String  studentAnswer;
    private String  correctAnswer;
    private boolean correct;
    private String  feedback;       // AI-generated explanation or default message
    private String  subSkill;
}
