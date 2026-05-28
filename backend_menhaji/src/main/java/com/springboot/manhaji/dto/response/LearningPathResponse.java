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
public class LearningPathResponse {
    private Long id;
    private Long studentId;
    private String studentName;
    private String recommendations;
    private LocalDateTime generatedAt;
}
