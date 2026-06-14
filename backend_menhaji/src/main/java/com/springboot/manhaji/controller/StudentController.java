package com.springboot.manhaji.controller;

import com.springboot.manhaji.dto.response.ApiResponse;
import com.springboot.manhaji.dto.response.AvatarOptionResponse;
import com.springboot.manhaji.dto.response.StudentDashboardResponse;
import com.springboot.manhaji.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    // ─── Dashboard ────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<StudentDashboardResponse>> getDashboard(
            Authentication authentication) {

        Long studentId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(studentService.getDashboard(studentId)));
    }

    // ─── Avatar ───────────────────────────────────────────────────────────────

    @PutMapping("/avatar")
    public ResponseEntity<ApiResponse<String>> updateAvatar(
            Authentication authentication,
            @RequestBody Map<String, String> body) {

        Long studentId = (Long) authentication.getPrincipal();
        studentService.updateAvatar(studentId, body.get("avatarId"));
        return ResponseEntity.ok(ApiResponse.success("تم تحديث الشخصية بنجاح"));
    }

    @GetMapping("/unlocked-avatars")
    public ResponseEntity<ApiResponse<List<AvatarOptionResponse>>> getUnlockedAvatars(
            Authentication authentication) {

        Long studentId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(studentService.getUnlockedAvatars(studentId)));
    }

    // ─── Grade ────────────────────────────────────────────────────────────────

    @PutMapping("/grade")
    public ResponseEntity<ApiResponse<String>> updateGrade(
            Authentication authentication,
            @RequestBody Map<String, Integer> body) {

        Long studentId = (Long) authentication.getPrincipal();
        Integer gradeLevel = body.get("gradeLevel");
        if (gradeLevel == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("gradeLevel مطلوب"));
        }
        studentService.updateGrade(studentId, gradeLevel);
        return ResponseEntity.ok(ApiResponse.success("تم تحديث الصف بنجاح"));
    }

    // ─── Password ─────────────────────────────────────────────────────────────

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            Authentication authentication,
            @RequestBody Map<String, String> body) {

        Long studentId = (Long) authentication.getPrincipal();
        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");
        if (currentPassword == null || newPassword == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("currentPassword و newPassword مطلوبان"));
        }
        studentService.changePassword(studentId, currentPassword, newPassword);
        return ResponseEntity.ok(ApiResponse.success("تم تغيير كلمة المرور بنجاح"));
    }
}
