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
     * STUDENT accounts: studentId param is ignored.
     * PARENT accounts: pass ?studentId=<students.id> for the linked child.
     * ADMIN accounts: pass ?studentId=<students.id> to act on any student.
     */
    @GetMapping("/{lessonId}")
    public ResponseEntity<ApiResponse<AdaptiveQuizPayload>> generateQuiz(
            @PathVariable Long lessonId,
            @RequestParam(required = false) Long studentId,
            Authentication authentication) {
        AdaptiveQuizPayload payload = adaptiveQuizService.generateAdaptiveQuiz(lessonId, authentication, studentId);
        return ResponseEntity.ok(ApiResponse.success(payload));
    }

    /**
     * Submit answers for a completed adaptive quiz session.
     * PARENT/ADMIN: ownership is verified automatically via the attempt's student.
     */
    @PostMapping("/{attemptId}/submit")
    public ResponseEntity<ApiResponse<AdaptiveQuizResult>> submitQuiz(
            @PathVariable Long attemptId,
            @Valid @RequestBody AdaptiveSubmitRequest request,
            Authentication authentication) {
        AdaptiveQuizResult result = adaptiveQuizService.submitAdaptiveQuiz(attemptId, request, authentication);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Request a hint for one question in an active adaptive quiz.
     * Level 1 = vague directional clue, level 3 = near-answer.
     */
    @GetMapping("/{attemptId}/question/{questionIndex}/hint")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHint(
            @PathVariable Long attemptId,
            @PathVariable int questionIndex,
            @RequestParam(defaultValue = "1") int level,
            Authentication authentication) {
        Map<String, Object> hint = adaptiveQuizService.getHintForAdaptiveQuestion(
                attemptId, questionIndex, level, authentication);
        return ResponseEntity.ok(ApiResponse.success(hint));
    }

    /**
     * Fetch an AI-generated explanation for one question after the quiz is submitted.
     */
    @GetMapping("/{attemptId}/question/{questionIndex}/explanation")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getExplanation(
            @PathVariable Long attemptId,
            @PathVariable int questionIndex,
            Authentication authentication) {
        Map<String, Object> explanation = adaptiveQuizService.getAnswerExplanation(
                attemptId, questionIndex, authentication);
        return ResponseEntity.ok(ApiResponse.success(explanation));
    }
}
