package com.springboot.manhaji.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectMasterySummary {
    private Long subjectId;
    private String subjectName;
    private Integer totalLessons;
    private Integer lessonsCompleted;
    private Double averageMastery;
}
