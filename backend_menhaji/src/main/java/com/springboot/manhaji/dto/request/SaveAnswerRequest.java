package com.springboot.manhaji.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SaveAnswerRequest {

    @NotNull(message = "Question ID is required")
    private Long questionId;

    @NotNull(message = "Lesson ID is required")
    private Long lessonId;

    private String answer;

    private String questionType;
}
