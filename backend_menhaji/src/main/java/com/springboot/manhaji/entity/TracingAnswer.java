package com.springboot.manhaji.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "tracing_answers",
    indexes = {
        @Index(name = "idx_tracing_answer_student",  columnList = "student_id"),
        @Index(name = "idx_tracing_answer_question", columnList = "question_id"),
        @Index(name = "idx_tracing_answer_student_question", columnList = "student_id, question_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TracingAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    /**
     * Raw drawing points submitted by Flutter, stored as JSON.
     * Format: [{"x":45.5,"y":12.3,"t":0},{"x":46.1,"y":14.8,"t":50}, ...]
     * Coordinates are 0-100 normalized; t is milliseconds since stroke start.
     */
    @Column(columnDefinition = "JSON")
    private String drawingPointsJson;

    /** Accuracy (0-100) as reported by the Flutter client (CustomPainter heuristic). */
    @Column
    private Double clientAccuracy;

    /**
     * Accuracy (0-100) re-calculated by the backend using the expected path.
     * -1 means calculation was skipped (no drawing points submitted).
     */
    @Column
    private Double serverAccuracy;

    /**
     * The authoritative final accuracy used for scoring.
     * Blend of server + client accuracies when both are available;
     * falls back to client accuracy when drawing points are absent.
     */
    @Column(nullable = false)
    private Double finalAccuracy;

    /** Score awarded for this attempt (0-100). */
    @Column(nullable = false)
    private Integer score;

    /** Stars earned: 0 = failed, 1 = pass, 2 = good, 3 = excellent. */
    @Column(nullable = false)
    private Integer stars;

    @Column(nullable = false)
    private Boolean isCorrect;

    /** 1-based attempt counter for this student/question pair. */
    @Column(nullable = false)
    private Integer attemptNumber;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Column(nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    @PrePersist
    protected void onCreate() {
        if (submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
    }
}
