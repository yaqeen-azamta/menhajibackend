package com.springboot.manhaji.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TracingResultResponse {

    private boolean success;

    private Long answerId;
    private Long questionId;

    /** The character that was traced. */
    private String character;

    /** Accuracy re-calculated by the backend using the expected path (0-100). */
    private Double serverAccuracy;

    /** Accuracy reported by Flutter's CustomPainter heuristic (0-100). */
    private Double clientAccuracy;

    /**
     * Authoritative final accuracy used for scoring decisions (0-100).
     * Blend of server + client when both are available.
     */
    private Double finalAccuracy;

    /** Score awarded for this attempt (0-100). */
    private Integer score;

    /** Stars earned: 0 = failed, 1 = pass, 2 = good, 3 = excellent. */
    private Integer stars;

    private Boolean isCorrect;

    /** 1-based attempt number for this student/question pair. */
    private Integer attemptNumber;

    /** Encouraging feedback message in Arabic. */
    private String feedback;

    /** Minimum accuracy the student needed to pass this question. */
    private Double requiredAccuracy;
}
