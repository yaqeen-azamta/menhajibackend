package com.springboot.manhaji.dto.response;

import com.springboot.manhaji.entity.enums.CompletionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonSummaryResponse {
    private Long id;
    private String title;
    private Integer orderIndex;
    private Integer semesterNumber;
    private CompletionStatus completionStatus;
    private Double masteryLevel;
}
