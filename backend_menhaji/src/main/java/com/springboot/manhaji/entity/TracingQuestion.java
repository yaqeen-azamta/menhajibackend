package com.springboot.manhaji.entity;

import com.springboot.manhaji.entity.enums.TracingCharacterType;
import com.springboot.manhaji.entity.enums.TracingLanguage;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tracing_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TracingQuestion {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "question_id")
    private Question question;

    /**
     * SVG path string for the character shape, in a 0-100 viewBox coordinate system.
     * Example: "M 50 10 L 50 90 M 38 90 L 62 90"
     * Flutter uses this to render the guide tracing overlay.
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String svgPath;

    /**
     * JSON array of {x, y} key points along the ideal tracing path,
     * in a 0-100 normalized coordinate system.
     * Example: [{"x":50,"y":10},{"x":50,"y":50},{"x":50,"y":90}]
     * Used by the backend accuracy engine to re-evaluate student drawings.
     */
    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String expectedPointsJson;

    /**
     * Tolerance percentage applied to reduce the required accuracy threshold.
     * E.g. tolerancePercentage=20 + expectedAccuracy=70 → actual required = 70*(1-0.20) = 56.
     */
    @Column(nullable = false)
    private Double tolerancePercentage = 20.0;

    /**
     * Minimum accuracy (0–100) that counts as a correct tracing, before tolerance.
     */
    @Column(nullable = false)
    private Double expectedAccuracy = 70.0;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private TracingLanguage language;

    /** The character this question teaches: "1", "A", "ب", etc. */
    
    @Column(name = "character", length = 10, nullable = false)
private String character;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private TracingCharacterType characterType;

    /** Human-readable label shown in the app UI, e.g. "Number One", "Letter A". */
    @Column(length = 120)
    private String displayName;

    /** How many separate strokes make up this character. */
    @Column(nullable = false)
    private Integer strokeCount = 1;

    /**
     * Optional JSON describing the ordered strokes (for animated stroke-order hints).
     * Example: [{"order":1,"path":"M 50 10 L 50 90"},{"order":2,"path":"M 38 90 L 62 90"}]
     */
    @Column(columnDefinition = "LONGTEXT")
    private String strokeOrderJson;

    /** URL to a reference image (PNG/SVG) shown beside the tracing canvas. */
    @Column(length = 512)
    private String guideImageUrl;
}
