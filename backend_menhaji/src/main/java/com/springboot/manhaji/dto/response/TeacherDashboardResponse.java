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
public class TeacherDashboardResponse {
    private Long teacherId;
    private String fullName;
    private String department;
    private Integer assignedGrade;

    private Integer totalStudents;
    private Integer activeThisWeek;
    private Integer lessonsCompletedTotal;
    private Double averageMasteryAcrossClass;

    private List<ClassStudentSummary> topStudents;
}
