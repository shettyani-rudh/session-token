package com.security.session_token.controller;

import com.security.session_token.dto.*;
import com.security.session_token.service.AuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest request) {
        try {
            // This matches your AuthService.signup(SignupRequest req)
            authService.signup(request);

            // Return proper JSON
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("message", "User created successfully"));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req,
                                               HttpServletRequest httpRequest) {

        // Extract User-Agent from request if not provided in body
        if (req.getUserAgent() == null || req.getUserAgent().isBlank()) {
            String userAgent = httpRequest.getHeader("User-Agent");
            req.setUserAgent(userAgent);
        }

        LoginResponse resp = authService.login(req);

        // set refresh token cookie
        ResponseCookie cookie = ResponseCookie.from("refresh_token", resp.getRefreshToken())
                .httpOnly(true)
                .secure(false) // set true when using HTTPS
                .sameSite("Strict")
                .path("/")
                .maxAge(resp.getRefreshTokenExpirySeconds())
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(resp);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refresh(@RequestBody RefreshTokenRequest request) {
        if (request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        TokenRefreshResponse resp = authService.refreshTokens(request.getRefreshToken());

        ResponseCookie cookie = ResponseCookie.from("refresh_token", resp.getRefreshToken())
                .httpOnly(true)
                .secure(false) // set true when using HTTPS
                .sameSite("Strict")
                .path("/")
                .maxAge(resp.getRefreshTokenExpirySeconds())
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(resp);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody LogoutRequest request) {
        if (request.getRefreshToken() != null && !request.getRefreshToken().isBlank()) {
            authService.revokeRefreshToken(request.getRefreshToken());
        }
        // clear cookie
        ResponseCookie cookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(Map.of("message", "Logged out successfully"));
    }
}