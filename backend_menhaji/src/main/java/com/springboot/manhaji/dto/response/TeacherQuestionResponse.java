package com.springboot.manhaji.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TeacherQuestionResponse {

    private Long id;
    private String questionText;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String correctAnswer;
    private Integer difficultyLevel;
    private String type;
    private Integer gradeLevel;

    private Long subjectId;
    private String subjectName;

    private Long lessonId;
    private String lessonTitle;

    private Long teacherId;
    private String teacherName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
