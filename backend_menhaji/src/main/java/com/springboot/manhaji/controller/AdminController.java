package com.springboot.manhaji.controller;

import com.springboot.manhaji.config.AiConfigProperties;
import com.springboot.manhaji.dto.response.AdminStatsResponse;
import com.springboot.manhaji.dto.response.ApiResponse;
import com.springboot.manhaji.dto.response.QuestionBankResponse;
import com.springboot.manhaji.dto.response.SubjectSummary;
import com.springboot.manhaji.dto.response.UserSummaryResponse;
import com.springboot.manhaji.entity.enums.Role;
import com.springboot.manhaji.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final AiConfigProperties aiConfig;
    private final Environment environment;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getStats()));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserSummaryResponse>>> getUsers(
            @RequestParam(required = false) Role role) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getAllUsers(role)));
    }

    // ==================== Question Bank (FR-9, unrestricted) ====================

    @GetMapping("/subjects")
    public ResponseEntity<ApiResponse<List<SubjectSummary>>> getSubjects(
            @RequestParam(required = false) Integer grade) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getAllSubjects(grade)));
    }

    @GetMapping("/ai-status")
    public ResponseEntity<Map<String, Object>> getAiStatus() {
        String rawKey = aiConfig.getGemini().getApiKey();
        boolean configured = aiConfig.getGemini().isConfigured();
        String maskedKey = (rawKey == null || rawKey.length() <= 6)
                ? rawKey
                : rawKey.substring(0, 6) + "***";

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("gemini_available", configured);
        status.put("gemini_key_raw_masked", maskedKey);
        status.put("gemini_key_is_not_set", "not-set".equals(rawKey));
        status.put("gemini_key_is_placeholder", rawKey != null && rawKey.startsWith("REPLACE_"));
        status.put("gemini_model", aiConfig.getGemini().getModel());
        status.put("active_spring_profiles", Arrays.asList(environment.getActiveProfiles()));
        status.put("fix_instructions", configured
                ? "Gemini is configured — no action needed."
                : "Set GEMINI_API_KEY env var and restart, OR edit application-local.yaml and start with -Dspring.profiles.active=local");
        return ResponseEntity.ok(status);
    }

    @GetMapping("/subjects/{subjectId}/questions")
    public ResponseEntity<ApiResponse<QuestionBankResponse>> getQuestionsForSubject(
            @PathVariable Long subjectId,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false) Long lessonId) {
        return ResponseEntity.ok(ApiResponse.success(
                adminService.getQuestionsForSubject(subjectId, difficulty, lessonId)));
    }
}
