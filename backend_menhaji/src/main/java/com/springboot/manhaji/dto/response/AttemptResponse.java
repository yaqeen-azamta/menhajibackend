package com.springboot.manhaji.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AttemptResponse {
    private Long attemptId;
    private Long quizId;
    private String status;
    private Double score;
    private int totalQuestions;
    private int correctAnswers;
    private int pointsEarned;
    private LocalDateTime submittedAt;
    private List<AnswerFeedback> answers;
}
