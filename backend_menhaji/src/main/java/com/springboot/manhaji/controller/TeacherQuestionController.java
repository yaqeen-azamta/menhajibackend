package com.springboot.manhaji.controller;

import com.springboot.manhaji.dto.request.TeacherQuestionRequest;
import com.springboot.manhaji.dto.response.ApiResponse;
import com.springboot.manhaji.dto.response.TeacherQuestionResponse;
import com.springboot.manhaji.service.TeacherQuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teacher/questions")
@RequiredArgsConstructor
@Slf4j
public class TeacherQuestionController {

    private final TeacherQuestionService teacherQuestionService;

    // POST /api/teacher/questions
    @PostMapping
    public ResponseEntity<ApiResponse<TeacherQuestionResponse>> createQuestion(
            @Valid @RequestBody TeacherQuestionRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Teacher userId={} creating question for lesson={}", userId, request.getLessonId());
        TeacherQuestionResponse response = teacherQuestionService.createQuestion(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Question created successfully", response));
    }

    // GET /api/teacher/questions
    @GetMapping
    public ResponseEntity<ApiResponse<List<TeacherQuestionResponse>>> getAllQuestions(
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(teacherQuestionService.getAllQuestions(userId)));
    }

    // GET /api/teacher/questions/grade/{gradeLevel}
    @GetMapping("/grade/{gradeLevel}")
    public ResponseEntity<ApiResponse<List<TeacherQuestionResponse>>> getByGrade(
            @PathVariable Integer gradeLevel,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(
                teacherQuestionService.getQuestionsByGrade(userId, gradeLevel)));
    }

    // GET /api/teacher/questions/subject/{subjectId}
    @GetMapping("/subject/{subjectId}")
    public ResponseEntity<ApiResponse<List<TeacherQuestionResponse>>> getBySubject(
            @PathVariable Long subjectId,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(
                teacherQuestionService.getQuestionsBySubject(userId, subjectId)));
    }

    // GET /api/teacher/questions/lesson/{lessonId}
    @GetMapping("/lesson/{lessonId}")
    public ResponseEntity<ApiResponse<List<TeacherQuestionResponse>>> getByLesson(
            @PathVariable Long lessonId,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(
                teacherQuestionService.getQuestionsByLesson(userId, lessonId)));
    }

    // PUT /api/teacher/questions/{id}
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TeacherQuestionResponse>> updateQuestion(
            @PathVariable Long id,
            @Valid @RequestBody TeacherQuestionRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Teacher userId={} updating question id={}", userId, id);
        TeacherQuestionResponse response = teacherQuestionService.updateQuestion(userId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Question updated successfully", response));
    }

    // DELETE /api/teacher/questions/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Teacher userId={} deleting question id={}", userId, id);
        teacherQuestionService.deleteQuestion(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Question deleted successfully", null));
    }
}
