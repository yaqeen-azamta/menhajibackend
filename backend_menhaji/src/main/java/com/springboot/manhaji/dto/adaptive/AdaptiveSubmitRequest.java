package com.springboot.manhaji.dto.adaptive;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

/** Request body for POST /api/quiz/adaptive/{attemptId}/submit. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdaptiveSubmitRequest {

    @NotEmpty(message = "answers must not be empty")
    @Valid
    private List<AdaptiveAnswerItem> answers;
}
