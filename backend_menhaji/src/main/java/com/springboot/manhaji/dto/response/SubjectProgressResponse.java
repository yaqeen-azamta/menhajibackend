package com.springboot.manhaji.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubjectProgressResponse {
    private Long subjectId;
    private String subjectName;
    private int totalLessons;
    private int completedLessons;
    private double masteryPercent;
}
