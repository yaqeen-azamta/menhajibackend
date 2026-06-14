package com.springboot.manhaji.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SaveAnswerResponse {

    // Boolean wrapper (not primitive) so Jackson serializes the field name as
    // "isCorrect" rather than stripping the "is" prefix it would from a boolean getter.
    @JsonProperty("isCorrect")
    private Boolean isCorrect;

    private double score;
    private String feedback;

    // Only set on wrong answers — reveals the correct answer so Flutter can
    // show it in the result card without sending it in the question payload.
    private String correctAnswer;

    // Points added to student.totalPoints for this submission:
    // Difficulty-based (5/10/15/20/25) on first-time correct; 0 on wrong or duplicate.
    private int pointsAwarded;
}
