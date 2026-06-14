package com.springboot.manhaji.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "adaptive_responses",
    indexes = {
        @Index(name = "idx_ar_attempt", columnList = "attempt_id")
    })
@Getter
@Setter
@NoArgsConstructor
public class AdaptiveResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private AdaptiveQuizAttempt attempt;

    /** 0-based position of this question in the attempt's generatedQuestionsJson array. */
    @Column(nullable = false)
    private int questionIndex;

    /** Snapshot of the question text — preserved even if the quiz is regenerated. */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String questionText;

    @Column(nullable = false, length = 32)
    private String questionType;

    @Column(length = 64)
    private String subSkill;

    @Column(columnDefinition = "TEXT")
    private String studentAnswer;

    @Column(columnDefinition = "TEXT")
    private String correctAnswer;

    @Column
    private Boolean isCorrect;

    @Column(columnDefinition = "TEXT")
    private String feedback;
}
