package com.springboot.manhaji.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TracingHistoryEntry {

    private Long answerId;
    private Long questionId;
    private String character;
    private String displayName;

    private Double finalAccuracy;
    private Double serverAccuracy;
    private Double clientAccuracy;

    private Integer score;
    private Integer stars;
    private Boolean isCorrect;
    private Integer attemptNumber;

    private String feedback;
    private LocalDateTime submittedAt;
}
