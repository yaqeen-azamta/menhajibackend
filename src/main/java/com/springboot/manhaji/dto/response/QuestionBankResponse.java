package com.springboot.manhaji.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response shape for the teacher/admin question-bank viewer. Bundles the
 * filtered questions with the set of lessons present in the unfiltered
 * result so the UI can populate a lesson dropdown even after a filter
 * hides most rows.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionBankResponse {
    private Long subjectId;
    private String subjectName;
    private Integer gradeLevel;
    private List<LessonSummary> lessons;
    private List<QuestionBankItem> questions;
    private Integer totalQuestionsInSubject;
}
