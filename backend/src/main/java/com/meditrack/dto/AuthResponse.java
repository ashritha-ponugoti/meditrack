package com.meditrack.dto;

import lombok.Getter;

@Getter
public class AuthResponse {
    private final String token;
    private final String username;
    private final String role;

    public AuthResponse(String token, String username, String role) {
        this.token = token;
        this.username = username;
        this.role = role;
    }
}
