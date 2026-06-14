package com.springboot.manhaji.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "reading_assessment_results",
        indexes = {
                @Index(name = "idx_rar_student",        columnList = "student_id"),
                @Index(name = "idx_rar_lesson",         columnList = "lesson_id"),
                @Index(name = "idx_rar_student_lesson", columnList = "student_id, lesson_id"),
                @Index(name = "idx_rar_student_created",columnList = "student_id, created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReadingAssessmentResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    // ─── Text content ──────────────────────────────────────────────────────────

    /** Snapshot of lesson.content at attempt time — preserved if the lesson is later edited. */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String originalText;

    /** Raw transcript returned by Gemini. NULL when the speech service was unavailable. */
    @Column(columnDefinition = "TEXT")
    private String recognizedText;

    // ─── Scalar scores ─────────────────────────────────────────────────────────

    /** Word-level accuracy: 0–100. */
    @Column(nullable = false)
    private Integer accuracy;

    /**
     * Number of non-empty words in originalText.
     * Denominator for accuracy — allows comparing scores across lessons of different lengths.
     */
    @Column(nullable = false)
    private Integer totalWords;

    /**
     * Denormalized count of correctly-read words.
     * Stored as an integer so SQL aggregations (AVG, SUM) work without JSON parsing.
     */
    @Column(nullable = false)
    private Integer correctWordCount;

    /**
     * Denormalized count of extra/substituted words the student said.
     * Stored as an integer for fast SQL aggregation.
     */
    @Column(nullable = false)
    private Integer incorrectWordCount;

    /**
     * Denormalized count of original words the student skipped.
     * Stored as an integer for fast SQL aggregation.
     */
    @Column(nullable = false)
    private Integer missingWordCount;

    // ─── Word lists (JSON arrays) ───────────────────────────────────────────────

    /** JSON array of words the student read correctly. Example: ["الكلب","يلعب","في"] */
    @Column(columnDefinition = "LONGTEXT")
    private String correctWordsJson;

    /**
     * JSON array of extra words the student said that had no match in the original.
     * Example: ["الحديقه"]
     */
    @Column(columnDefinition = "LONGTEXT")
    private String incorrectWordsJson;

    /**
     * JSON array of original words absent from the student's reading.
     * The most important column for "most frequently missed words" analytics.
     * Example: ["الحديقة"]
     */
    @Column(columnDefinition = "LONGTEXT")
    private String missingWordsJson;

    // ─── Attempt metadata ──────────────────────────────────────────────────────

    /**
     * Identifies the AI service that produced recognizedText.
     * Allows quality comparison when the transcription backend changes.
     * Known values: "gemini", "whisper", "azure-stt", "google-stt", "unavailable"
     */
    @Column(nullable = false, length = 50)
    private String transcriptionEngine;

    /**
     * "ar" or "en".
     * Required for language-specific analytics, teacher reports, and Phase 2 pronunciation scoring.
     */
    @Column(nullable = false, length = 10)
    private String language;

    // ─── Phase 2: pronunciation scoring (reserved — NULL in Phase 1) ───────────

    /**
     * Overall pronunciation quality score (0.0–100.0) from PronunciationAssessmentService.
     * NULL until a Phase 2 implementation bean is wired.
     *
     * TODO: populate via PronunciationAssessmentService.assess() in ReadingService when available.
     */
    @Column
    private Double pronunciationScore;

    /**
     * Per-word phoneme scoring JSON from PronunciationAssessmentService.
     * LONGTEXT because per-phoneme detail for a 50-word paragraph can be several kilobytes.
     * NULL until a Phase 2 implementation bean is wired.
     *
     * TODO: populate via PronunciationAssessmentService.assess() in ReadingService when available.
     */
    @Column(columnDefinition = "LONGTEXT")
    private String pronunciationDetailJson;

    // ─── Timestamp ─────────────────────────────────────────────────────────────

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
