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
public class ChildSummaryResponse {
    private Long studentId;
    private String fullName;
    private String avatarId;
    private Integer gradeLevel;
    private Integer totalPoints;
    private Integer currentStreak;
    private Integer lessonsCompleted;
    private Integer totalLessons;
    private Double overallMastery;
    private LocalDateTime lastLoginAt;
}
