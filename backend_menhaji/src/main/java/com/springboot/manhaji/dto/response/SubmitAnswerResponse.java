package com.springboot.manhaji.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubmitAnswerResponse {
    private Long questionId;
    private boolean isCorrect;
    private String feedback;
    private String correctAnswer;
    private int pointsEarned;
}
