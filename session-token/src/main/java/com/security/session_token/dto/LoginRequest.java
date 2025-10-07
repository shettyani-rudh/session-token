package com.security.session_token.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
    private String userAgent; // Add this field for security fingerprinting
}