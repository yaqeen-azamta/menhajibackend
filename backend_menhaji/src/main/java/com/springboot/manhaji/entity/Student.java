package com.springboot.manhaji.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String studentName;

    @Column
    private Integer gradeLevel;

    @Column
    private String avatarId;

    @Column(nullable = false)
    private Integer currentStreak = 0;

    @Column(nullable = false)
    private Integer totalPoints = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_lesson_id")
    private Lesson currentLesson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id")
    private School school;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Progress> progressRecords = new ArrayList<>();

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Attempt> attempts = new ArrayList<>();

    @OneToOne(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    private LearningPath learningPath;
}
