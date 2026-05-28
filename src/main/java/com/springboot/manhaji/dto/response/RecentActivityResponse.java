package com.springboot.manhaji.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class RecentActivityResponse {
    private String type;           // LESSON_VIEWED, QUIZ_COMPLETED
    private String title;
    private String subjectName;
    private Double score;
    private Integer pointsEarned;
    private LocalDateTime timestamp;
}
