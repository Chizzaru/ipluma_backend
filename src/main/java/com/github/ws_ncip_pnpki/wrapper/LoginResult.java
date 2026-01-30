package com.github.ws_ncip_pnpki.wrapper;

import com.github.ws_ncip_pnpki.dto.AuthResponse;
import lombok.Getter;

public class LoginResult {
    private final String accessToken;
    private final String refreshToken;
    private final AuthResponse authResponse;

    public LoginResult(String accessToken, String refreshToken, AuthResponse authResponse) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.authResponse = authResponse;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public AuthResponse getAuthResponse() {
        return authResponse;
    }
}