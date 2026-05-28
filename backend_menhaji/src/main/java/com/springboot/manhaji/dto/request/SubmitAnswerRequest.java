package com.springboot.manhaji.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitAnswerRequest {
    @NotNull(message = "Question ID is required")
    private Long questionId;

    private String answer;         // Text answer for MCQ/TRUE_FALSE
    private String spokenText;     // Transcribed speech for SHORT_ANSWER
    private String audioRef;       // Reference to audio file if voice answer
}
