package com.springboot.manhaji.controller;

import com.springboot.manhaji.dto.response.ApiResponse;
import com.springboot.manhaji.dto.response.LessonResponse;
import com.springboot.manhaji.dto.response.LessonSummaryResponse;
import com.springboot.manhaji.dto.response.SubjectResponse;
import com.springboot.manhaji.service.LessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;

    @GetMapping("/subjects")
    public ResponseEntity<ApiResponse<List<SubjectResponse>>> getSubjects(
            @RequestParam Integer gradeLevel,
            Authentication authentication) {
        Long studentId = (Long) authentication.getPrincipal();
        List<SubjectResponse> subjects = lessonService.getSubjectsByGrade(gradeLevel, studentId);
        return ResponseEntity.ok(ApiResponse.success(subjects));
    }

    @GetMapping("/subject/{subjectId}")
    public ResponseEntity<ApiResponse<List<LessonSummaryResponse>>> getLessonsBySubject(
            @PathVariable Long subjectId,
            Authentication authentication) {
        Long studentId = (Long) authentication.getPrincipal();
        List<LessonSummaryResponse> lessons = lessonService.getLessonsBySubject(subjectId, studentId);
        return ResponseEntity.ok(ApiResponse.success(lessons));
    }

    @GetMapping("/{lessonId}")
    public ResponseEntity<ApiResponse<LessonResponse>> getLessonDetail(
            @PathVariable Long lessonId,
            Authentication authentication) {
        Long studentId = (Long) authentication.getPrincipal();
        LessonResponse lesson = lessonService.getLessonDetail(lessonId, studentId);
        return ResponseEntity.ok(ApiResponse.success(lesson));
    }
}
