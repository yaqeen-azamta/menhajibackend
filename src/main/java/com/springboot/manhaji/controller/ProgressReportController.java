package com.springboot.manhaji.controller;

import com.springboot.manhaji.dto.response.ApiResponse;
import com.springboot.manhaji.dto.response.LearningPathResponse;
import com.springboot.manhaji.dto.response.ProgressReportResponse;
import com.springboot.manhaji.service.LearningPathService;
import com.springboot.manhaji.service.ProgressReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ProgressReportController {

    private final ProgressReportService reportService;
    private final LearningPathService learningPathService;

    @PostMapping("/progress")
    public ResponseEntity<ApiResponse<ProgressReportResponse>> generateReport(Authentication authentication) {
        Long studentId = (Long) authentication.getPrincipal();
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(reportService.generateReport(studentId)));
    }

    @GetMapping("/progress")
    public ResponseEntity<ApiResponse<List<ProgressReportResponse>>> getReports(Authentication authentication) {
        Long studentId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(reportService.getReports(studentId)));
    }

    @PostMapping("/learning-path")
    public ResponseEntity<ApiResponse<LearningPathResponse>> generatePath(Authentication authentication) {
        Long studentId = (Long) authentication.getPrincipal();
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(learningPathService.generatePath(studentId)));
    }

    @GetMapping("/learning-path")
    public ResponseEntity<ApiResponse<LearningPathResponse>> getPath(Authentication authentication) {
        Long studentId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(learningPathService.getPath(studentId)));
    }
}
