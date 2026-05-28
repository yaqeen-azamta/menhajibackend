package com.springboot.manhaji.controller;

import com.springboot.manhaji.dto.request.LoginRequest;
import com.springboot.manhaji.dto.request.PhoneLoginRequest;
import com.springboot.manhaji.dto.request.RegisterRequest;
import com.springboot.manhaji.dto.response.ApiResponse;
import com.springboot.manhaji.dto.response.AuthResponse;
import com.springboot.manhaji.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.loginWithEmail(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/login/phone")
    public ResponseEntity<ApiResponse<AuthResponse>> loginWithPhone(@Valid @RequestBody PhoneLoginRequest request) {
        AuthResponse response = authService.loginWithPhone(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Refresh token is required"));
        }
        AuthResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse>> me(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Not authenticated"));
        }
        Long userId = (Long) authentication.getPrincipal();
        // getCurrentUser returns the concrete subtype (Student, Teacher …) via JOINED inheritance
        AuthResponse response = authService.buildAuthResponseForUser(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
