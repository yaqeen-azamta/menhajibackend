package com.springboot.manhaji.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectSummary {
    private Long id;
    private String name;
    private Integer gradeLevel;
    private Integer lessonCount;
    private Integer questionCount;
}
