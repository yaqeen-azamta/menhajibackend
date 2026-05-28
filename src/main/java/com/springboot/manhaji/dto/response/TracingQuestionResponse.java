package com.springboot.manhaji.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.springboot.manhaji.entity.enums.TracingCharacterType;
import com.springboot.manhaji.entity.enums.TracingLanguage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TracingQuestionResponse {

    private Long questionId;
    private Long lessonId;

    /** The character to trace: "1", "A", "ب", etc. */
    private String character;

    /** Human-readable label, e.g. "Number One", "Letter A", "حرف الباء". */
    private String displayName;

    private TracingLanguage language;
    private TracingCharacterType characterType;
    private Integer difficultyLevel;

    /**
     * SVG path string in a 0-100 viewBox coordinate system.
     * Flutter scales this to the actual canvas size.
     * Example: "M 50 10 L 50 90 M 38 90 L 62 90"
     */
    private String svgPath;

    /**
     * JSON array of ideal key points (0-100 coords) used by the backend verifier.
     * Flutter may also use these to render the animated guide dots.
     * Example: [{"x":50,"y":10},{"x":50,"y":90}]
     */
    private String expectedPointsJson;

    /** Minimum accuracy % the student must reach to pass (before tolerance). */
    private Double expectedAccuracy;

    /** Tolerance reduction applied to {@code expectedAccuracy}. */
    private Double tolerancePercentage;

    /** Effective required accuracy = expectedAccuracy * (1 - tolerancePercentage/100). */
    private Double effectiveRequiredAccuracy;

    /** Number of strokes needed to draw the character. */
    private Integer strokeCount;

    /**
     * JSON array describing each stroke in order, for animated hint support.
     * Example: [{"order":1,"path":"M 50 10 L 50 90"},{"order":2,"path":"M 38 90 L 62 90"}]
     */
    private String strokeOrderJson;

    /** URL to the reference image shown beside the tracing canvas. */
    private String guideImageUrl;

    /** Audio URL that pronounces the character name (if available). */
    private String audioUrl;
}
