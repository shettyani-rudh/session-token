package com.security.session_token.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
@Data
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Column(nullable = false, unique = true)
    private String tokenHash;

    private Instant createdAt;
    private Instant lastUsedAt;
    private Instant expiresAt;
    private boolean revoked;

    private String userAgentHash;
}
