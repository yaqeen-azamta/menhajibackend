package com.springboot.manhaji.controller;

import com.springboot.manhaji.dto.request.SaveAnswerRequest;
import com.springboot.manhaji.dto.response.ApiResponse;
import com.springboot.manhaji.dto.response.SaveAnswerResponse;
import com.springboot.manhaji.service.StudentAnswerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student-answers")
@RequiredArgsConstructor
@Slf4j
public class StudentQuestionAnswerController {

    private final StudentAnswerService studentAnswerService;

    @PostMapping
    public ResponseEntity<ApiResponse<SaveAnswerResponse>> saveAnswer(
            @Valid @RequestBody SaveAnswerRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("saveAnswer: userId={}, questionId={}, lessonId={}",
                userId, request.getQuestionId(), request.getLessonId());
        SaveAnswerResponse response = studentAnswerService.saveAnswer(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
