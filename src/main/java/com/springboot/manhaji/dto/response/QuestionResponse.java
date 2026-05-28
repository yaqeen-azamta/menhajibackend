package com.springboot.manhaji.dto.response;

import com.springboot.manhaji.dto.question.QuestionOption;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class QuestionResponse {

    private Long id;

    private String type;

    private String questionText;

   private List<QuestionOption> options;

    private int difficultyLevel;

    private String subSkill;

    private String imageUrl;

    private String audioUrl;
}