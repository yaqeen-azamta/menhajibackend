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
public class RecommendedLessonResponse {
    private Long lessonId;
    private String title;
    private Long subjectId;
    private String subjectName;
    private Integer orderIndex;
    private CompletionStatus completionStatus;
}
