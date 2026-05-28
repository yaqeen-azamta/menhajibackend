package com.springboot.manhaji.controller;

import com.springboot.manhaji.dto.response.ApiResponse;
import com.springboot.manhaji.dto.response.ParentDashboardResponse;
import com.springboot.manhaji.dto.response.StudentDetailResponse;
import com.springboot.manhaji.service.ParentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/parent")
@RequiredArgsConstructor
public class ParentController {

    private final ParentService parentService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<ParentDashboardResponse>> getDashboard(Authentication authentication) {
        Long parentId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(parentService.getDashboard(parentId)));
    }

    @GetMapping("/children/{childId}")
    public ResponseEntity<ApiResponse<StudentDetailResponse>> getChildDetail(
            Authentication authentication,
            @PathVariable Long childId) {
        Long parentId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(parentService.getChildDetail(parentId, childId)));
    }
}
