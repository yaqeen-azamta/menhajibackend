package com.springboot.manhaji.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassStudentSummary {
    private Long studentId;
    private String fullName;
    private String email;
    private Integer gradeLevel;
    private Integer totalPoints;
    private Integer currentStreak;
    private Integer lessonsCompleted;
    private Integer lessonsInProgress;
    private Double averageMastery;
    private LocalDateTime lastLoginAt;
}
