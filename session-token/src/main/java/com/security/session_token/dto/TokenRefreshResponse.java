package com.security.session_token.dto;



import lombok.Data;

@Data
public class TokenRefreshResponse {
    private String accessToken;
    private String refreshToken;
    private int accessTokenExpirySeconds;
    private int refreshTokenExpirySeconds;
}
