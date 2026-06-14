package com.springboot.manhaji.controller;

import com.springboot.manhaji.dto.response.ApiResponse;
import com.springboot.manhaji.dto.response.LeaderboardEntryResponse;
import com.springboot.manhaji.dto.response.ProgressSummaryResponse;
import com.springboot.manhaji.service.ProgressService;
import com.springboot.manhaji.service.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
@Slf4j
public class ProgressController {

    private final ProgressService progressService;
    private final StudentService  studentService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<ProgressSummaryResponse>> getProgressSummary(
            @RequestParam(required = false) Long studentId,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        Long resolvedStudentId = studentService.resolveStudent(authentication, studentId).getId();
        log.info("GET /progress/summary — userId={}, requestedStudentId={}, resolvedStudentId={}",
                userId, studentId, resolvedStudentId);

        ProgressSummaryResponse summary = progressService.getProgressSummary(resolvedStudentId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @PostMapping("/lesson/{lessonId}/complete")
    public ResponseEntity<ApiResponse<Map<String, Object>>> completeLesson(
            @PathVariable Long lessonId,
            @RequestParam(required = false) Long studentId,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        Long resolvedStudentId = studentService.resolveStudent(authentication, studentId).getId();
        log.info("POST /progress/lesson/{}/complete — userId={}, requestedStudentId={}, resolvedStudentId={}",
                lessonId, userId, studentId, resolvedStudentId);

        Map<String, Object> result = progressService.completeLesson(resolvedStudentId, lessonId);
        return ResponseEntity.ok(ApiResponse.success("تم إكمال الدرس بنجاح", result));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<ApiResponse<List<LeaderboardEntryResponse>>> getLeaderboard(
            @RequestParam(required = false) Integer gradeLevel,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        List<LeaderboardEntryResponse> leaderboard = progressService.getLeaderboard(userId, gradeLevel);
        return ResponseEntity.ok(ApiResponse.success(leaderboard));
    }
}
