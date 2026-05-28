package com.springboot.manhaji.dto.request;

import com.springboot.manhaji.entity.enums.QuestionType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateQuestionRequest {

    private QuestionType type;

    private String questionText;

    private String correctAnswer;

    // JSON
    private String options;

    private Integer difficultyLevel;

    private String subSkill;

    private Long lessonId;
}