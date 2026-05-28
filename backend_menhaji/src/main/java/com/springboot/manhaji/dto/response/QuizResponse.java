package com.springboot.manhaji.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class QuizResponse {
    private Long id;
    private String title;
    private boolean gamified;
    private int totalQuestions;
    private List<QuestionResponse> questions;
    private String lessonContent;
    private String lessonObjectives;
    private List<String> lessonImageUrls;
}
