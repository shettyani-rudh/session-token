package com.security.session_token.repository;

import com.security.session_token.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository  // ← ADD THIS
public interface UserRepository extends JpaRepository<User, Long> {  // ← CHANGE to JpaRepository
    Optional<User> findByUsername(String username);
}