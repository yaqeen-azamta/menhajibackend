package com.springboot.manhaji.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsResponse {
    private long totalStudents;
    private long totalTeachers;
    private long totalParents;
    private long totalAdmins;
    private long totalSubjects;
    private long totalLessons;
    private long totalAttempts;
    private long totalCompletedLessons;
    private long activeStudentsThisWeek;
}
