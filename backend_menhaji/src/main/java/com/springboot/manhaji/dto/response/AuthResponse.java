package com.springboot.manhaji.dto.response;

import com.springboot.manhaji.entity.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private Long userId;      // users.id — matches the JWT "userId" claim
    private Long studentId;   // students.id — only set when role == STUDENT; null otherwise
    private String fullName;
    private String email;
    private String phone;
    private Role role;
    private Integer gradeLevel;
    private String avatarId;
}
