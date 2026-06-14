package com.springboot.manhaji.dto.request;

import com.springboot.manhaji.entity.enums.QuestionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TeacherQuestionRequest {

    @NotBlank(message = "Question text is required")
    private String questionText;

    @NotBlank(message = "Option A is required")
    private String optionA;

    @NotBlank(message = "Option B is required")
    private String optionB;

    // Optional: required for 3- or 4-option questions
    private String optionC;
    private String optionD;

    @NotBlank(message = "Correct answer is required")
    private String correctAnswer;

    @NotNull(message = "Difficulty level is required")
    @Min(value = 1, message = "Difficulty level must be between 1 and 3")
    @Max(value = 3, message = "Difficulty level must be between 1 and 3")
    private Integer difficultyLevel;

    // Defaults to MCQ in the service if not supplied.
    private QuestionType type;

    @NotNull(message = "Subject ID is required")
    private Long subjectId;

    @NotNull(message = "Lesson ID is required")
    private Long lessonId;
}
