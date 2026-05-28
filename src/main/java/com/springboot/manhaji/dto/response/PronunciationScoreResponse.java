package com.springboot.manhaji.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PronunciationScoreResponse {
    private Long questionId;
    private String expectedText;
    private String transcribedText;
    private int score;
    private String rating;
    private String feedback;
    private boolean isCorrect;
    private int pointsEarned;
}
