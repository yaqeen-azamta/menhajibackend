package com.springboot.manhaji.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * Tracing submission payload sent by Flutter after the student finishes drawing.
 *
 * <p>The client MUST send {@code questionId} and {@code isCorrect}.
 * All other fields are optional but improve backend accuracy verification:</p>
 * <ul>
 *   <li>{@code drawingPoints} – normalized (0-100) touch coordinates recorded during drawing.
 *       When present the backend re-calculates accuracy server-side.</li>
 *   <li>{@code clientAccuracy} – accuracy calculated by Flutter's CustomPainter heuristic.
 *       Used as a fallback when drawing points are absent.</li>
 *   <li>{@code score} / {@code stars} – client-computed values stored alongside the
 *       server result for audit purposes.</li>
 * </ul>
 *
 * <p>Existing callers (QuizService) only set {@code questionId}, {@code score},
 * {@code stars}, {@code isCorrect}, and {@code feedback} – all new fields are optional
 * so backward compatibility is preserved.</p>
 */
@Data
public class TracingSubmitRequest {

    @NotNull(message = "Question ID is required")
    private Long questionId;

    @Min(value = 0, message = "Score must be >= 0")
    @Max(value = 100, message = "Score must be <= 100")
    private Integer score;

    @Min(value = 0, message = "Stars must be >= 0")
    @Max(value = 3, message = "Stars must be <= 3")
    private Integer stars;

    @NotNull(message = "isCorrect is required")
    private Boolean isCorrect;

    private String feedback;

    // ── Extended fields for backend accuracy verification ──────────────────────

    /**
     * Ordered list of touch points recorded during the tracing stroke(s).
     * Coordinates must be in the normalized 0-100 space matching the expected path.
     */
    @Valid
    private List<DrawingPoint> drawingPoints;

    /**
     * Accuracy (0-100) as calculated by Flutter's CustomPainter heuristic.
     * Sent alongside {@code drawingPoints} for comparison / audit.
     */
    @DecimalMin(value = "0.0", message = "clientAccuracy must be >= 0")
    @DecimalMax(value = "100.0", message = "clientAccuracy must be <= 100")
    private Double clientAccuracy;

    // ── Inner DTO ──────────────────────────────────────────────────────────────

    @Data
    public static class DrawingPoint {

        /** X coordinate in 0-100 normalized canvas space. */
        @NotNull(message = "DrawingPoint.x is required")
        private Double x;

        /** Y coordinate in 0-100 normalized canvas space. */
        @NotNull(message = "DrawingPoint.y is required")
        private Double y;

        /** Milliseconds since the start of this stroke (optional, for velocity analysis). */
        private Long t;
    }
}

