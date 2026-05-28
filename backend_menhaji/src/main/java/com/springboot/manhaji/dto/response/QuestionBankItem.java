package com.springboot.manhaji.dto.response;

import com.springboot.manhaji.entity.enums.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionBankItem {
    private Long id;
    private QuestionType type;
    private String questionText;
    private String correctAnswer;
    /** Parsed from the Question.options JSON field; null/empty when not applicable. */
    private List<String> options;
    private Integer difficultyLevel;
    private String subSkill;
    private String imageUrl;
    private String audioUrl;
    private Long lessonId;
    private String lessonTitle;
}
