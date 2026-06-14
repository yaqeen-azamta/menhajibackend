package com.springboot.manhaji.controller;

import com.springboot.manhaji.dto.request.SaveAnswerRequest;
import com.springboot.manhaji.dto.response.ApiResponse;
import com.springboot.manhaji.dto.response.SaveAnswerResponse;
import com.springboot.manhaji.entity.Student;
import com.springboot.manhaji.service.StudentAnswerService;
import com.springboot.manhaji.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student-answers")
@RequiredArgsConstructor
@Slf4j
public class StudentQuestionAnswerController {

    private final StudentAnswerService studentAnswerService;
    private final StudentService       studentService;

    /**
     * Save a student's answer to a practice question.
     * STUDENT: no extra param needed.
     * PARENT/ADMIN: pass ?studentId=<students.id> of the target student.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<SaveAnswerResponse>> saveAnswer(
            @Valid @RequestBody SaveAnswerRequest request,
            @RequestParam(required = false) Long studentId,
            Authentication authentication) {

        Student student = studentService.resolveStudent(authentication, studentId);
        log.info("saveAnswer: userId={}, resolvedStudentId={}, questionId={}, lessonId={}",
                authentication.getPrincipal(), student.getId(),
                request.getQuestionId(), request.getLessonId());

        SaveAnswerResponse response = studentAnswerService.saveAnswer(student, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
