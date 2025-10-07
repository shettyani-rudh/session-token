package com.security.session_token.repository;

import com.security.session_token.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository  // ← ADD THIS ANNOTATION
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {  // ← CHANGE to JpaRepository
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.userId = :userId")  // ← Use named parameter
    void revokeAllByUserId(@Param("userId") Long userId);  // ← ADD @Param annotation
}