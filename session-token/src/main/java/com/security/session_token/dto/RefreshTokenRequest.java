package com.security.session_token.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RefreshTokenRequest {
    // Getters and setters
    private String refreshToken;

    // Default constructor
    public RefreshTokenRequest() {}

    // Constructor with parameter
    public RefreshTokenRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

}