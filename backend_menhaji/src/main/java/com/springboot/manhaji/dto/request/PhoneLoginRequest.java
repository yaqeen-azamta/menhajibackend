package com.springboot.manhaji.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PhoneLoginRequest {
    @NotBlank(message = "Phone number is required")
    private String phone;

    @NotBlank(message = "Password is required")
    private String password;
}
