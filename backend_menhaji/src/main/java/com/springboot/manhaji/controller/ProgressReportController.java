package com.springboot.manhaji.controller;

import com.springboot.manhaji.dto.response.ApiResponse;
import com.springboot.manhaji.dto.response.LearningPathResponse;
import com.springboot.manhaji.dto.response.ProgressReportResponse;
import com.springboot.manhaji.service.LearningPathService;
import com.springboot.manhaji.service.ProgressReportService;
import com.springboot.manhaji.service.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
public class ProgressReportController {

    private final ProgressReportService reportService;
    private final LearningPathService learningPathService;
    private final StudentService studentService;

    /**
     * Generates (or regenerates) the AI progress report for the target student.
     *
     * STUDENT accounts: no extra param needed.
     * PARENT/ADMIN accounts: pass ?studentId=<students.id> of the target student.
     */
    @PostMapping("/progress")
    public ResponseEntity<ApiResponse<ProgressReportResponse>> generateReport(
            @RequestParam(required = false) Long studentId,
            Authentication authentication) {

        Long resolvedId = studentService.resolveStudent(authentication, studentId).getId();
        log.info("POST /reports/progress — resolvedStudentId={}", resolvedId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(reportService.generateReport(resolvedId)));
    }

    /**
     * Returns all saved progress reports for the target student.
     *
     * STUDENT accounts: no extra param needed.
     * PARENT/ADMIN accounts: pass ?studentId=<students.id> of the target student.
     */
    @GetMapping("/progress")
    public ResponseEntity<ApiResponse<List<ProgressReportResponse>>> getReports(
            @RequestParam(required = false) Long studentId,
            Authentication authentication) {

        Long resolvedId = studentService.resolveStudent(authentication, studentId).getId();
        log.info("GET /reports/progress — resolvedStudentId={}", resolvedId);

        return ResponseEntity.ok(ApiResponse.success(reportService.getReports(resolvedId)));
    }

    /**
     * Generates (or updates) the AI learning path for the target student.
     *
     * STUDENT accounts: no extra param needed.
     * PARENT/ADMIN accounts: pass ?studentId=<students.id> of the target student.
     */
    @PostMapping("/learning-path")
    public ResponseEntity<ApiResponse<LearningPathResponse>> generatePath(
            @RequestParam(required = false) Long studentId,
            Authentication authentication) {

        Long resolvedId = studentService.resolveStudent(authentication, studentId).getId();
        log.info("POST /reports/learning-path — resolvedStudentId={}", resolvedId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(learningPathService.generatePath(resolvedId)));
    }

    /**
     * Returns the saved learning path for the target student.
     *
     * STUDENT accounts: no extra param needed.
     * PARENT/ADMIN accounts: pass ?studentId=<students.id> of the target student.
     */
    @GetMapping("/learning-path")
    public ResponseEntity<ApiResponse<LearningPathResponse>> getPath(
            @RequestParam(required = false) Long studentId,
            Authentication authentication) {

        Long resolvedId = studentService.resolveStudent(authentication, studentId).getId();
        log.info("GET /reports/learning-path — resolvedStudentId={}", resolvedId);

        return ResponseEntity.ok(ApiResponse.success(learningPathService.getPath(resolvedId)));
    }
}
