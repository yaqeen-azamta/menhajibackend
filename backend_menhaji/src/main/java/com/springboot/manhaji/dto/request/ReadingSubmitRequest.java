package com.springboot.manhaji.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Payload sent by Flutter after a READING question is evaluated via
 * {@code POST /api/reading/assess}. The client forwards the accuracy and
 * star count from the reading-assessment response so the backend can persist
 * a {@code StudentResponse} row for progress dashboards and AI reports.
 *
 * <p>The actual word-level detail is already stored in
 * {@code reading_assessment_results}; this endpoint only adds the quiz-context
 * record (attempt ↔ question ↔ isCorrect) needed by the analytics queries.</p>
 */
@Data
public class ReadingSubmitRequest {

    @NotNull(message = "Question ID is required")
    private Long questionId;

    /** Accuracy percentage returned by {@code POST /api/reading/assess} (0-100). */
    @Min(value = 0, message = "Accuracy must be >= 0")
    @Max(value = 100, message = "Accuracy must be <= 100")
    private Integer accuracy;

    /** Star rating computed client-side from accuracy (0-3). */
    @Min(value = 0, message = "Stars must be >= 0")
    @Max(value = 3, message = "Stars must be <= 3")
    private Integer stars;

    /**
     * Whether this reading attempt is considered correct.
     * The client applies accuracy >= threshold (default 60 %) logic.
     */
    @NotNull(message = "isCorrect is required")
    private Boolean isCorrect;
}
