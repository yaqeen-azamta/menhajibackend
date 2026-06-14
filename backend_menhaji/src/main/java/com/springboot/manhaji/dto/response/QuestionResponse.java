package com.springboot.manhaji.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.springboot.manhaji.dto.question.QuestionOption;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuestionResponse {

    private Long id;
    private String type;
    private String questionText;
    private List<QuestionOption> options;
    private int difficultyLevel;
    private String subSkill;
    private String imageUrl;
    private String audioUrl;

    // Only populated for READING questions (holds the passage text).
    // Null for all other types so the answer is never leaked to the client.
    private String correctAnswer;
}
