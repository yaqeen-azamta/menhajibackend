package com.springboot.manhaji.controller;

import com.springboot.manhaji.dto.response.ApiResponse;
import com.springboot.manhaji.dto.response.AvatarOptionResponse;
import com.springboot.manhaji.dto.response.StudentDashboardResponse;
import com.springboot.manhaji.service.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
@Slf4j
public class StudentController {

    private final StudentService studentService;

    // ─── Dashboard ────────────────────────────────────────────────────────────

    /**
     * Returns the student dashboard.
     *
     * STUDENT accounts: no extra param needed — the student is resolved from the JWT.
     * PARENT  accounts: pass {@code ?studentId=<student.id>} for the child to view.
     *                   This is the child's {@code students.id}, NOT their {@code users.id}.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<StudentDashboardResponse>> getDashboard(
            @RequestParam(required = false) Long studentId,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        log.info("GET /api/student/dashboard — authenticatedUserId={}, requestedStudentId={}", userId, studentId);

        return ResponseEntity.ok(ApiResponse.success(studentService.getDashboard(authentication, studentId)));
    }

    // ─── Avatar ───────────────────────────────────────────────────────────────

    /**
     * Updates the avatar for the authenticated student only.
     * Parents cannot change a child's avatar through this endpoint.
     */
    @PutMapping("/avatar")
    public ResponseEntity<ApiResponse<String>> updateAvatar(
            Authentication authentication,
            @RequestBody Map<String, String> body) {

        Long userId = (Long) authentication.getPrincipal();
        studentService.updateAvatar(userId, body.get("avatarId"));
        return ResponseEntity.ok(ApiResponse.success("تم تحديث الشخصية بنجاح"));
    }

    /**
     * Returns unlocked avatars.
     *
     * STUDENT accounts: no extra param needed.
     * PARENT  accounts: pass {@code ?studentId=<student.id>} to view a child's unlocked avatars.
     */
    @GetMapping("/unlocked-avatars")
    public ResponseEntity<ApiResponse<List<AvatarOptionResponse>>> getUnlockedAvatars(
            @RequestParam(required = false) Long studentId,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        log.info("GET /api/student/unlocked-avatars — authenticatedUserId={}, requestedStudentId={}", userId, studentId);

        return ResponseEntity.ok(ApiResponse.success(studentService.getUnlockedAvatars(authentication, studentId)));
    }

    // ─── Grade ────────────────────────────────────────────────────────────────

    /**
     * Updates the grade level for the authenticated student only.
     * Parents cannot change a child's grade through this endpoint.
     */
    @PutMapping("/grade")
    public ResponseEntity<ApiResponse<String>> updateGrade(
            Authentication authentication,
            @RequestBody Map<String, Integer> body) {

        Long userId = (Long) authentication.getPrincipal();
        Integer gradeLevel = body.get("gradeLevel");
        if (gradeLevel == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("gradeLevel مطلوب"));
        }
        studentService.updateGrade(userId, gradeLevel);
        return ResponseEntity.ok(ApiResponse.success("تم تحديث الصف بنجاح"));
    }

    // ─── Password ─────────────────────────────────────────────────────────────

    /**
     * Changes the password for the authenticated student only.
     * Parents cannot change a child's password through this endpoint.
     */
    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            Authentication authentication,
            @RequestBody Map<String, String> body) {

        Long userId = (Long) authentication.getPrincipal();
        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");
        if (currentPassword == null || newPassword == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("currentPassword و newPassword مطلوبان"));
        }
        studentService.changePassword(userId, currentPassword, newPassword);
        return ResponseEntity.ok(ApiResponse.success("تم تغيير كلمة المرور بنجاح"));
    }
}
