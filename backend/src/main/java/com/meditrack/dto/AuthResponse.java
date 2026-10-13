package com.meditrack.dto;

import lombok.Getter;

@Getter
public class AuthResponse {
    private final String token;
    private final String username;
    private final String role;
    private final Long pharmacyId;

    public AuthResponse(String token, String username, String role, Long pharmacyId) {
        this.token = token;
        this.username = username;
        this.role = role;
        this.pharmacyId = pharmacyId;
    }
}
