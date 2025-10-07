package com.security.session_token.dto;

public class LogoutRequest {
    private String refreshToken;

    // Default constructor
    public LogoutRequest() {}

    // Constructor with parameter
    public LogoutRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    // Getters and setters
    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}