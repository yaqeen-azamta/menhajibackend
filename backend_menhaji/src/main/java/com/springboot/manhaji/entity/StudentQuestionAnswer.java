package com.springboot.manhaji.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "student_question_answers",
    indexes = {
        @Index(name = "idx_sqa_student_lesson", columnList = "student_id, lesson_id"),
        @Index(name = "idx_sqa_student_question", columnList = "student_id, question_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentQuestionAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(columnDefinition = "TEXT")
    private String answerText;

    @Column(nullable = false)
    private Boolean isCorrect = false;

    @Column(nullable = false)
    private Double score = 0.0;

    @Column(nullable = false)
    private LocalDateTime answeredAt;

    @Column(columnDefinition = "TEXT")
    private String feedback;
}
