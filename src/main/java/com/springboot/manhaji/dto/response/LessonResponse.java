package com.springboot.manhaji.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonResponse {
    private Long id;
    private String title;
    private String content;
    private String audioUrl;
    private List<String> imageUrls;
    private String objectives;
    private Integer orderIndex;
    private Integer semesterNumber;
    private Long subjectId;
    private String subjectName;
    private Integer gradeLevel;
    private Integer totalQuestions;
}
