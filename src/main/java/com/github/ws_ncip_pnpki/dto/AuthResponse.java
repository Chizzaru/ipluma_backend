package com.github.ws_ncip_pnpki.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
public class AuthResponse {
    private Long id;
    private String username;
    private String email;
    private Set<String> roles;
    private String accessToken;    // Make sure this is here
    private String refreshToken;   // Make sure this is here

    public AuthResponse(Long id, String username, String email, Set<String> roles) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.roles = roles;
    }

    // Constructor with tokens
    public AuthResponse(Long id, String username, String email, Set<String> roles,
                        String accessToken, String refreshToken) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.roles = roles;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
