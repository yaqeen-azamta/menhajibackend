package com.springboot.manhaji.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentDashboardResponse {
    private Long studentId;
    private String fullName;
    private String avatarId;
    private Integer gradeLevel;
    private Integer currentStreak;
    private Integer totalPoints;
    private List<SubjectResponse> subjects;
}
