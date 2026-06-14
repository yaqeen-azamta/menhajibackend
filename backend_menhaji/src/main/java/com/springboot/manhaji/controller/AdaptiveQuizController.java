package com.springboot.manhaji.controller;

import com.springboot.manhaji.dto.adaptive.AdaptiveQuizPayload;
import com.springboot.manhaji.dto.adaptive.AdaptiveQuizResult;
import com.springboot.manhaji.dto.adaptive.AdaptiveSubmitRequest;
import com.springboot.manhaji.dto.response.ApiResponse;
import com.springboot.manhaji.service.AdaptiveQuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/quiz/adaptive")
@RequiredArgsConstructor
public class AdaptiveQuizController {

    private final AdaptiveQuizService adaptiveQuizService;

    /**
     * Generate (or resume) an adaptive quiz for the given lesson.
     * Returns questions WITHOUT correct answers.
     */
    @GetMapping("/{lessonId}")
    public ResponseEntity<ApiResponse<AdaptiveQuizPayload>> generateQuiz(
            @PathVariable Long lessonId,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        AdaptiveQuizPayload payload = adaptiveQuizService.generateAdaptiveQuiz(lessonId, userId);
        return ResponseEntity.ok(ApiResponse.success(payload));
    }

    /**
     * Submit answers for a completed adaptive quiz session.
     * Returns per-question feedback, score, and updated skill profiles.
     */
    @PostMapping("/{attemptId}/submit")
    public ResponseEntity<ApiResponse<AdaptiveQuizResult>> submitQuiz(
            @PathVariable Long attemptId,
            @Valid @RequestBody AdaptiveSubmitRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        AdaptiveQuizResult result = adaptiveQuizService.submitAdaptiveQuiz(attemptId, request, userId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Request a hint for one question in an active adaptive quiz.
     * Level 1 = vague, level 3 = near-answer.
     */
    @GetMapping("/{attemptId}/question/{questionIndex}/hint")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHint(
            @PathVariable Long attemptId,
            @PathVariable int questionIndex,
            @RequestParam(defaultValue = "1") int level,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Map<String, Object> hint = adaptiveQuizService.getHintForAdaptiveQuestion(
                attemptId, questionIndex, level, userId);
        return ResponseEntity.ok(ApiResponse.success(hint));
    }
}
