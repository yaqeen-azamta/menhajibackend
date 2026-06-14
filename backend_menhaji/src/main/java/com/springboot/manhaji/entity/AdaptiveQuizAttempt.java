package com.springboot.manhaji.entity;

import com.springboot.manhaji.entity.enums.AttemptStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "adaptive_quiz_attempts",
    indexes = {
        @Index(name = "idx_aqa_student",              columnList = "student_id"),
        @Index(name = "idx_aqa_student_lesson_status", columnList = "student_id, lesson_id, status")
    })
@Getter
@Setter
@NoArgsConstructor
public class AdaptiveQuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    /** Difficulty level (1-5) used when Gemini generated this quiz. */
    @Column(nullable = false)
    private int difficultyLevel = 1;

    /** JSON array of subSkill names that were identified as weak and targeted. */
    @Column(columnDefinition = "LONGTEXT")
    private String focusSkillsJson;

    /**
     * Full JSON of List<GeneratedQuestion> including correctAnswer fields.
     * Stored server-side only — NEVER sent to the client.
     * Used to evaluate submitted answers without re-calling Gemini.
     */
    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String generatedQuestionsJson;

    @Column(nullable = false)
    private int questionCount = 0;

    @Column(nullable = false)
    private int correctCount = 0;

    /** 0–100 percentage score, populated after grading. */
    @Column
    private Double score;

    @Column(nullable = false, updatable = false)
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime completedAt;

    /** "GEMINI" | "FALLBACK_DB" — records where the questions came from. */
    @Column(length = 16, nullable = false)
    private String quizSource = "GEMINI";

    /** Total hint requests across all questions in this attempt. Enforced limit: 10. */
    @Column(nullable = false)
    private int totalHintsUsed = 0;

    /**
     * JSON map of questionIndex (String key) → hint requests used (Integer).
     * E.g. {"0":2,"3":1}. Enforced per-question limit: 3.
     */
    @Column(columnDefinition = "LONGTEXT")
    private String hintUsageJson = "{}";

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AdaptiveResponse> responses = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.startedAt = LocalDateTime.now();
    }
}
