package com.security.session_token.dto;



import lombok.Data;

@Data
public class SignupRequest {
    private String username;
    private String password;
}
