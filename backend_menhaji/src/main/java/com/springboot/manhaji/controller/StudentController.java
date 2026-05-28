package com.springboot.manhaji.controller;

import com.springboot.manhaji.dto.response.ApiResponse;
import com.springboot.manhaji.dto.response.StudentDashboardResponse;
import com.springboot.manhaji.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    // ─────────────────────────────────────────────
    // STUDENT DASHBOARD
    // ─────────────────────────────────────────────
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<StudentDashboardResponse>> getDashboard(
            Authentication authentication
    ) {

        Long studentId = (Long) authentication.getPrincipal();

        StudentDashboardResponse dashboard =
                studentService.getDashboard(studentId);

        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }

    // ─────────────────────────────────────────────
    // UPDATE AVATAR
    // ─────────────────────────────────────────────
    @PutMapping("/avatar")
    public ResponseEntity<ApiResponse<String>> updateAvatar(
            Authentication authentication,
            @RequestBody Map<String, String> body
    ) {

        Long studentId = (Long) authentication.getPrincipal();

        String avatarId = body.get("avatarId");

        studentService.updateAvatar(studentId, avatarId);

        return ResponseEntity.ok(
                ApiResponse.success("Avatar updated successfully")
        );
    }
}