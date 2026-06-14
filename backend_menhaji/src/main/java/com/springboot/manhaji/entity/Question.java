package com.springboot.manhaji.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.springboot.manhaji.entity.enums.QuestionType;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionType type;

    @Column(columnDefinition = "TEXT")
    private String questionText;

    @Column(nullable = false)
    private String correctAnswer;

    // JSON STRING — stored as LONGTEXT (MariaDB/MySQL 5.5 compatible)
    @Column(columnDefinition = "LONGTEXT")
    private String options;

    @Column(nullable = false)
    private Integer difficultyLevel = 1;

    @Column(length = 32)
    private String subSkill;

    // QUESTION IMAGE
    @Column(length = 512)
    private String imageUrl;

    @Column(length = 512)
    private String audioUrl;

    @Column(length = 64)
    private String audioTextHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    @JsonIgnore
    private Lesson lesson;

    @ManyToMany(mappedBy = "questions")
    @JsonIgnore
    private List<Quiz> quizzes = new ArrayList<>();
}