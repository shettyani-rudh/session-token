package com.security.session_token.service;

import com.security.session_token.dto.*;
import com.security.session_token.entity.RefreshToken;
import com.security.session_token.entity.User;
import com.security.session_token.exception.TokenException;
import com.security.session_token.repository.RefreshTokenRepository;
import com.security.session_token.repository.UserRepository;
import com.security.session_token.util.HashUtils;
import com.security.session_token.util.TokenUtils;
import com.security.session_token.util.JwtUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenService tokenService;
    private final JwtUtils jwtUtils;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       TokenService tokenService,
                       JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenService = tokenService;
        this.jwtUtils = jwtUtils;
    }

    public void signup(SignupRequest req) {
        if (userRepository.findByUsername(req.getUsername()).isPresent()) {
            throw new IllegalArgumentException("username exists");
        }
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        userRepository.save(user);
    }

    public LoginResponse login(LoginRequest req) {
        User user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("invalid credentials"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("invalid credentials");
        }

        // issue access token (JWT) and refresh token
        String accessToken = jwtUtils.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = TokenUtils.generateOpaqueToken();
        String hash = HashUtils.sha256Hex(refreshToken);

        Instant now = Instant.now();
        Instant expiresAt = now.plus(30, ChronoUnit.MINUTES); // 30 minutes

        RefreshToken entity = new RefreshToken();
        entity.setUserId(user.getId());
        entity.setTokenHash(hash);
        entity.setCreatedAt(now);
        entity.setExpiresAt(expiresAt);
        entity.setRevoked(false);

        // SET USER_AGENT_HASH if userAgent is provided
        if (req.getUserAgent() != null && !req.getUserAgent().isBlank()) {
            String userAgentHash = HashUtils.sha256Hex(req.getUserAgent());
            entity.setUserAgentHash(userAgentHash);
        }

        refreshTokenRepository.save(entity);

        LoginResponse resp = new LoginResponse();
        resp.setAccessToken(accessToken);
        resp.setRefreshToken(refreshToken);
        resp.setRefreshTokenExpirySeconds(30 * 60); // 30 minutes in seconds
        resp.setAccessTokenExpirySeconds((int) JwtUtils.ACCESS_TOKEN_EXPIRATION_SECONDS);
        return resp;
    }

    public TokenRefreshResponse refreshTokens(String presentedRefreshToken) {
        String presentedHash = HashUtils.sha256Hex(presentedRefreshToken);
        Optional<RefreshToken> opt = refreshTokenRepository.findByTokenHash(presentedHash);

        if (opt.isEmpty()) {
            throw new TokenException("invalid refresh token");
        }

        RefreshToken existing = opt.get();

        // UPDATE LAST_USED_AT when token is used
        existing.setLastUsedAt(Instant.now());

        if (existing.isRevoked() || existing.getExpiresAt().isBefore(Instant.now())) {
            // Use manual revocation instead of broken repository method
            revokeAllUserTokensManually(existing.getUserId());
            throw new TokenException("invalid or revoked refresh token");
        }

        // rotate: revoke existing and create a new one
        existing.setRevoked(true);
        refreshTokenRepository.save(existing); // Save with last_used_at update

        String newRefresh = TokenUtils.generateOpaqueToken();
        String newHash = HashUtils.sha256Hex(newRefresh);

        RefreshToken newEntity = new RefreshToken();
        newEntity.setUserId(existing.getUserId());
        newEntity.setTokenHash(newHash);
        newEntity.setCreatedAt(Instant.now());
        newEntity.setExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES)); // 30 minutes
        newEntity.setRevoked(false);

        // Copy userAgentHash from existing token to new token
        if (existing.getUserAgentHash() != null) {
            newEntity.setUserAgentHash(existing.getUserAgentHash());
        }

        refreshTokenRepository.save(newEntity);

        // issue a new access token as well
        User user = userRepository.findById(existing.getUserId())
                .orElseThrow(() -> new IllegalStateException("user not found"));

        String newAccess = jwtUtils.generateAccessToken(user.getId(), user.getUsername());

        TokenRefreshResponse resp = new TokenRefreshResponse();
        resp.setAccessToken(newAccess);
        resp.setRefreshToken(newRefresh);
        resp.setRefreshTokenExpirySeconds(30 * 60); // 30 minutes in seconds
        resp.setAccessTokenExpirySeconds((int) JwtUtils.ACCESS_TOKEN_EXPIRATION_SECONDS);
        return resp;
    }

    public void revokeRefreshToken(String presentedRefreshToken) {
        String hash = HashUtils.sha256Hex(presentedRefreshToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(rt -> {
            // Use manual revocation instead of broken repository method
            revokeAllUserTokensManually(rt.getUserId());
        });
    }

    // NEW METHOD: Manual token revocation that actually works
    private void revokeAllUserTokensManually(Long userId) {
        try {
            System.out.println("DEBUG: Revoking all tokens for user: " + userId);

            int revokedCount = 0;
            for (RefreshToken token : refreshTokenRepository.findAll()) {
                if (token.getUserId().equals(userId) && !token.isRevoked()) {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                    revokedCount++;
                    System.out.println("DEBUG: Revoked token ID: " + token.getId());
                }
            }
            System.out.println("DEBUG: Successfully revoked " + revokedCount + " tokens");
        } catch (Exception e) {
            System.out.println("ERROR in revokeAllUserTokensManually: " + e.getMessage());
            throw new RuntimeException("Token revocation failed", e);
        }
    }
}