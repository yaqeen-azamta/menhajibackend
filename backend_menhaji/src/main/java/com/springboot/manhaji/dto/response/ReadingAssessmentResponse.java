package com.springboot.manhaji.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReadingAssessmentResponse {
    private String originalText;
    private String recognizedText;
    private int accuracy;
    private List<String> correctWords;
    private List<String> incorrectWords;
    private List<String> missingWords;
}
