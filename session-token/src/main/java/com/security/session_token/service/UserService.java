package com.security.session_token.service;


import com.security.session_token.entity.User;
import com.security.session_token.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    private final UserRepository repo;

    public UserService(UserRepository repo) {
        this.repo = repo;
    }

    public Optional<User> findById(Long id) {
        return repo.findById(id);
    }
}
