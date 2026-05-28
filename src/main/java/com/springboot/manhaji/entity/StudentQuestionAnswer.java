package com.springboot.manhaji.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "student_question_answers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentQuestionAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Student who answered
    @Column(nullable = false)
    private Long studentId;

    // Related question
    @Column(nullable = false)
    private Long questionId;

    // Lesson practice
    private Long lessonId;

    // Optional quiz
    private Long quizId;

    // For write answer questions
    @Column(columnDefinition = "TEXT")
    private String answerText;

    // For MCQ / TRUE_FALSE
    @Column(columnDefinition = "TEXT")
    private String selectedOption;

    // Result

@Column(name = "is_correct")
private Integer isCorrect = 0;

// Optional grading / AI score

private Double score = 0.0;
    private Integer attemptNumber = 1;

    // Timestamp
    @Column(nullable = false)
    private java.time.LocalDateTime answeredAt =
            java.time.LocalDateTime.now();
}