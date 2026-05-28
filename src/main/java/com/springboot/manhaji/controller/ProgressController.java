package com.springboot.manhaji.controller;

import com.springboot.manhaji.dto.response.ApiResponse;
import com.springboot.manhaji.dto.response.LeaderboardEntryResponse;
import com.springboot.manhaji.dto.response.ProgressSummaryResponse;
import com.springboot.manhaji.service.ProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<ProgressSummaryResponse>> getProgressSummary(
            Authentication authentication) {
        Long studentId = (Long) authentication.getPrincipal();
        ProgressSummaryResponse summary = progressService.getProgressSummary(studentId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @PostMapping("/lesson/{lessonId}/complete")
    public ResponseEntity<ApiResponse<Map<String, Object>>> completeLesson(
            @PathVariable Long lessonId,
            Authentication authentication) {
        Long studentId = (Long) authentication.getPrincipal();
        Map<String, Object> result = progressService.completeLesson(studentId, lessonId);
        return ResponseEntity.ok(ApiResponse.success("تم إكمال الدرس بنجاح", result));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<ApiResponse<List<LeaderboardEntryResponse>>> getLeaderboard(
            @RequestParam(required = false) Integer gradeLevel,
            Authentication authentication) {
        Long studentId = (Long) authentication.getPrincipal();
        List<LeaderboardEntryResponse> leaderboard = progressService.getLeaderboard(studentId, gradeLevel);
        return ResponseEntity.ok(ApiResponse.success(leaderboard));
    }
}
