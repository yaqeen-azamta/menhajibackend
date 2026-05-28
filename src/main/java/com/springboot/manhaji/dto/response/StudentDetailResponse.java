package com.springboot.manhaji.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentDetailResponse {
    private Long studentId;
    private String fullName;
    private String email;
    private String phone;
    private Integer gradeLevel;
    private Integer totalPoints;
    private Integer currentStreak;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;

    private Integer lessonsCompleted;
    private Integer lessonsInProgress;
    private Double overallMastery;
    private Integer totalAttempts;
    private Double averageScore;

    private List<SubjectMasterySummary> subjectBreakdown;
}
