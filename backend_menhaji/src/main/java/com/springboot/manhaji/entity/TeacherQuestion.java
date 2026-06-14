package com.springboot.manhaji.entity;

import com.springboot.manhaji.entity.enums.QuestionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "teacher_questions",
    indexes = {
        @Index(name = "idx_tq_teacher",       columnList = "teacher_id"),
        @Index(name = "idx_tq_lesson",        columnList = "lesson_id"),
        @Index(name = "idx_tq_teacher_grade", columnList = "teacher_id, grade_level")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String questionText;

    @Column(nullable = false)
    private String optionA;

    @Column(nullable = false)
    private String optionB;

    @Column
    private String optionC;

    @Column
    private String optionD;

    @Column(nullable = false)
    private String correctAnswer;

    @Column(nullable = false)
    @Builder.Default
    private Integer difficultyLevel = 1;

    // Reuses the shared QuestionType enum — keeps type vocabulary consistent
    // across curriculum questions and teacher-created questions.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private QuestionType type = QuestionType.MCQ;

    // Denormalized for fast grade-based filtering; derived from subject.gradeLevel on save.
    @Column(nullable = false)
    private Integer gradeLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
