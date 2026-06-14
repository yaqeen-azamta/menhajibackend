package com.springboot.manhaji.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "student_skill_profiles",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_ssp_student_lesson_skill",
        columnNames = {"student_id", "lesson_id", "sub_skill"}),
    indexes = {
        @Index(name = "idx_ssp_student",        columnList = "student_id"),
        @Index(name = "idx_ssp_student_lesson",  columnList = "student_id, lesson_id")
    })
@Getter
@Setter
@NoArgsConstructor
public class StudentSkillProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(name = "sub_skill", nullable = false, length = 64)
    private String subSkill;

    @Column(nullable = false)
    private int totalAttempts = 0;

    @Column(nullable = false)
    private int correctAnswers = 0;

    @Column(nullable = false)
    private int wrongAnswers = 0;

    /** 0–100, recomputed after every answer. */
    @Column(nullable = false)
    private double accuracy = 0.0;

    /**
     * Student's current difficulty for this sub-skill.
     * 1 = Beginner, 2 = Easy, 3 = Medium, 4 = Advanced, 5 = Expert.
     * Adjusted by consecutive-correct / consecutive-wrong streak rules.
     */
    @Column(nullable = false)
    private int currentDifficulty = 1;

    /** Resets to 0 whenever a wrong answer is recorded. */
    @Column(nullable = false)
    private int consecutiveCorrect = 0;

    /** Resets to 0 whenever a correct answer is recorded. */
    @Column(nullable = false)
    private int consecutiveWrong = 0;

    @Column
    private LocalDateTime lastAttemptAt;

    /**
     * Record one answer and apply adaptive difficulty rules.
     *
     * Difficulty-up rule : 5 consecutive correct answers → difficulty++ (max 5)
     * Difficulty-down rule: 4 consecutive wrong answers  → difficulty-- (min 1)
     */
    public void recordAnswer(boolean correct) {
        totalAttempts++;
        if (correct) {
            correctAnswers++;
            consecutiveCorrect++;
            consecutiveWrong = 0;
            if (consecutiveCorrect >= 5 && currentDifficulty < 5) {
                currentDifficulty++;
                consecutiveCorrect = 0;
            }
        } else {
            wrongAnswers++;
            consecutiveWrong++;
            consecutiveCorrect = 0;
            if (consecutiveWrong >= 4 && currentDifficulty > 1) {
                currentDifficulty--;
                consecutiveWrong = 0;
            }
        }
        accuracy = totalAttempts > 0 ? (correctAnswers * 100.0) / totalAttempts : 0.0;
        lastAttemptAt = LocalDateTime.now();
    }
}
