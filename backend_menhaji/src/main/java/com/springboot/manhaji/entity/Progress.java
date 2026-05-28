package com.springboot.manhaji.entity;

import com.springboot.manhaji.entity.enums.CompletionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "progress",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"student_id", "lesson_id"})
        },
        indexes = {
                @Index(name = "idx_progress_student", columnList = "student_id"),
                @Index(name = "idx_progress_lesson", columnList = "lesson_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Progress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double masteryLevel = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompletionStatus completionStatus = CompletionStatus.NOT_STARTED;

    @Column
    private LocalDateTime lastAccessedAt;

    @Column
    private LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;
}
