package com.springboot.manhaji.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AnswerFeedback {
    private Long questionId;
    private String questionText;
    private String studentAnswer;
    private String correctAnswer;
    private boolean isCorrect;
    private String feedback;
}
