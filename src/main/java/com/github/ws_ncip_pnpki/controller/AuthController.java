package com.github.ws_ncip_pnpki.controller;

import com.github.ws_ncip_pnpki.dto.*;
import com.github.ws_ncip_pnpki.model.RefreshToken;
import com.github.ws_ncip_pnpki.model.User;
import com.github.ws_ncip_pnpki.security.CustomUserDetails;
import com.github.ws_ncip_pnpki.security.CustomUserDetailsService;
import com.github.ws_ncip_pnpki.service.AuthService;
import com.github.ws_ncip_pnpki.service.RefreshTokenService;
import com.github.ws_ncip_pnpki.util.JwtUtil;
import com.github.ws_ncip_pnpki.wrapper.LoginResult;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${app.cookie.same-site:Lax}")
    private String cookieSameSite;

    @Value("${app.cookie.domain:}")
    private String cookieDomain;

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = authService.registerUser(request);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            System.out.println("=== LOGIN START ===");
            System.out.println("Request Origin: " + httpRequest.getHeader("Origin"));
            System.out.println("Cookie config - Secure: " + cookieSecure + ", SameSite: " + cookieSameSite + ", Domain: " + cookieDomain);

            LoginResult result = authService.login(request);

            System.out.println("Tokens generated successfully");
            System.out.println("Access token length: " + result.getAccessToken().length());
            System.out.println("Refresh token length: " + result.getRefreshToken().length());

            // Test with simple cookie first
            ResponseCookie testCookie = buildCookie("testCookie", "simple-test-value", false, 3600);

            System.out.println("Test cookie created: " + testCookie.toString());

            // Create actual cookies
            ResponseCookie accessTokenCookie = buildCookie("accessToken", result.getAccessToken(), true, 15 * 60);
            ResponseCookie refreshTokenCookie = buildCookie("refreshToken", result.getRefreshToken(), true, 7 * 24 * 60 * 60);

            System.out.println("=== ALL COOKIES ===");
            System.out.println("Test Cookie: " + testCookie.toString());
            System.out.println("Access Cookie: " + accessTokenCookie.toString());
            System.out.println("Refresh Cookie: " + refreshTokenCookie.toString());

            // Build headers
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.SET_COOKIE, testCookie.toString());
            headers.add(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
            headers.add(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

            System.out.println("=== RESPONSE HEADERS ===");
            headers.forEach((key, values) -> {
                System.out.println(key + ": " + values);
            });

            Map<String, Object> responseBody = Map.of(
                    "id", result.getAuthResponse().getId(),
                    "username", result.getAuthResponse().getUsername(),
                    "email", result.getAuthResponse().getEmail(),
                    "roles", result.getAuthResponse().getRoles(),
                    "debug", "cookies_should_be_set",
                    "finishedSetup", result.getAuthResponse().isFinishedSetup(),
                    "cookieConfig", Map.of(
                            "secure", cookieSecure,
                            "sameSite", cookieSameSite,
                            "domain", cookieDomain
                    )
            );

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(responseBody);

        } catch (Exception e) {
            System.out.println("Login error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Invalid username or password"));
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(
            @CookieValue(name = "refreshToken", required = false) String refreshToken) {

        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("Refresh token not found"));
        }

        try {
            // Verify refresh token
            RefreshToken validRefreshToken = refreshTokenService.verifyRefreshToken(refreshToken);
            CustomUserDetails userDetails = (CustomUserDetails) userDetailsService
                    .loadUserByUsername(validRefreshToken.getUser().getUsername());

            // Generate new access token
            String newAccessToken = jwtUtil.generateToken(userDetails);

            // Create new access token cookie
            ResponseCookie accessTokenCookie = buildCookie("accessToken", newAccessToken, true, 15 * 60);

            // Return user data
            Set<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());

            Map<String, Object> userData = Map.of(
                    "id", userDetails.getUser().getId(),
                    "username", userDetails.getUsername(),
                    "email", userDetails.getUser().getEmail(),
                    "roles", roles,
                    "finishedSetup", userDetails.getUser().isFinishedSetup()
            );

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                    .body(userData);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("Invalid refresh token"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CookieValue(name = "refreshToken", required = false) String refreshToken) {

        try {
            // Delete the specific refresh token (this is sufficient)
            if (refreshToken != null) {
                System.out.println("Deleting by token: " + refreshToken);
                refreshTokenService.deleteByToken(refreshToken);
                System.out.println("Deleted by token successfully");
            }

            // Clear cookies
            ResponseCookie clearAccess = clearCookie("accessToken");
            ResponseCookie clearRefresh = clearCookie("refreshToken");

            System.out.println("Cookies cleared, returning response");

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, clearAccess.toString())
                    .header(HttpHeaders.SET_COOKIE, clearRefresh.toString())
                    .body(new MessageResponse("Logged out successfully"));

        } catch (Exception e) {
            System.out.println("Logout error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Logout failed: " + e.getMessage()));
        }
    }

    @PostMapping("/debug-cookies")
    public ResponseEntity<?> debugCookies(HttpServletRequest request) {
        Map<String, Object> debugInfo = new HashMap<>();

        // Check all cookies
        Cookie[] cookies = request.getCookies();
        List<String> cookieNames = new ArrayList<>();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                cookieNames.add(cookie.getName() + "=" + cookie.getValue());
            }
        }
        debugInfo.put("cookies", cookieNames);
        debugInfo.put("cookieCount", cookies != null ? cookies.length : 0);

        // Check headers
        Enumeration<String> headerNames = request.getHeaderNames();
        Map<String, String> headers = new HashMap<>();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                headers.put(headerName, request.getHeader(headerName));
            }
        }
        debugInfo.put("headers", headers);

        return ResponseEntity.ok(debugInfo);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@CookieValue(name = "accessToken", required = false) String accessToken,
                                            @CookieValue(name = "refreshToken", required = false) String refreshToken) {
        try {
            // Try to validate access token first
            if (accessToken != null && jwtUtil.validateToken(accessToken)) {
                String username = jwtUtil.getUsernameFromToken(accessToken);
                CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(username);

                return ResponseEntity.ok(buildUserResponse(userDetails));
            }

            // If access token invalid but refresh token exists, refresh automatically
            if (refreshToken != null) {
                RefreshToken validRefreshToken = refreshTokenService.verifyRefreshToken(refreshToken);
                CustomUserDetails userDetails = (CustomUserDetails) userDetailsService
                        .loadUserByUsername(validRefreshToken.getUser().getUsername());

                // Generate new access token
                String newAccessToken = jwtUtil.generateToken(userDetails);

                // Set new access token cookie
                ResponseCookie accessTokenCookie = buildCookie("accessToken", newAccessToken, true, 15 * 60);

                return ResponseEntity.ok()
                        .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                        .body(buildUserResponse(userDetails));
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/set-test-cookie")
    public ResponseEntity<?> setTestCookie(HttpServletRequest request) {
        System.out.println("=== SET TEST COOKIE ===");
        System.out.println("Request Origin: " + request.getHeader("Origin"));
        System.out.println("Cookie config - Secure: " + cookieSecure + ", SameSite: " + cookieSameSite);

        // Test with configured SameSite
        ResponseCookie testCookie = createCookie("testCookieConfigured", "configured-value", false, 3600);

        // Test with None for comparison
        ResponseCookie noneCookie = ResponseCookie.from("testCookieNone", "none-value")
                .httpOnly(false)
                .secure(false) // None requires Secure=true in production, but false for HTTP dev
                .path("/")
                .maxAge(3600)
                .sameSite("None")
                .build();

        System.out.println("Configured Cookie: " + testCookie.toString());
        System.out.println("None Cookie: " + noneCookie.toString());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, testCookie.toString())
                .header(HttpHeaders.SET_COOKIE, noneCookie.toString())
                .body(Map.of(
                        "message", "Test cookies set",
                        "configuredCookie", testCookie.toString(),
                        "noneCookie", noneCookie.toString(),
                        "config", Map.of(
                                "secure", cookieSecure,
                                "sameSite", cookieSameSite,
                                "domain", cookieDomain
                        )
                ));
    }

    @GetMapping("/check-test-cookie")
    public ResponseEntity<?> checkTestCookie(
            @CookieValue(name = "testCookieConfigured", required = false) String configuredCookie,
            @CookieValue(name = "testCookieNone", required = false) String noneCookie,
            HttpServletRequest request) {

        System.out.println("=== CHECK TEST COOKIES ===");
        System.out.println("Received cookies - Configured: " + configuredCookie + ", None: " + noneCookie);

        return ResponseEntity.ok(Map.of(
                "testCookieConfigured", configuredCookie != null ? "present" : "missing",
                "testCookieNone", noneCookie != null ? "present" : "missing",
                "requestOrigin", request.getHeader("Origin")
        ));
    }

    // Helper methods
    private ResponseCookie buildCookie(String name, String value, boolean httpOnly, int maxAge) {
        return createCookie(name, value, httpOnly, maxAge);
    }

    private ResponseCookie createCookie(String name, String value, boolean httpOnly, int maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(httpOnly)
                .secure(cookieSecure)
                .path("/")  // ✅ Root path - works everywhere
                .maxAge(maxAge)
                .sameSite(cookieSameSite)
                .build();
    }

    private ResponseCookie clearCookie(String name) {
        // For clearing, build a new cookie with maxAge = 0
        return createCookie(name, "", true, 0);
    }

    private Map<String, Object> buildUserResponse(CustomUserDetails userDetails) {
        Set<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        return Map.of(
                "id", userDetails.getUser().getId(),
                "username", userDetails.getUsername(),
                "email", userDetails.getUser().getEmail(),
                "roles", roles,
                "finishedSetup", userDetails.getUser().isFinishedSetup()
        );
    }
}