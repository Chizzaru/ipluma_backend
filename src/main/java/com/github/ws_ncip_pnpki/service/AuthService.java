package com.github.ws_ncip_pnpki.service;

import com.github.ws_ncip_pnpki.dto.AuthResponse;
import com.github.ws_ncip_pnpki.dto.LoginRequest;
import com.github.ws_ncip_pnpki.dto.RegisterRequest;
import com.github.ws_ncip_pnpki.model.RefreshToken;
import com.github.ws_ncip_pnpki.model.Role;
import com.github.ws_ncip_pnpki.model.User;
import com.github.ws_ncip_pnpki.repository.RoleRepository;
import com.github.ws_ncip_pnpki.repository.UserRepository;
import com.github.ws_ncip_pnpki.security.CustomUserDetails;
import com.github.ws_ncip_pnpki.util.JwtUtil;
import com.github.ws_ncip_pnpki.wrapper.LoginResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    public User registerUser(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists!");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists!");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        Set<Role> roles = new HashSet<>();

        if (request.getRoles() == null || request.getRoles().isEmpty()) {
            Role userRole = roleRepository.findByName("ROLE_USER")
                    .orElseThrow(() -> new RuntimeException("Role not found"));
            roles.add(userRole);
        } else {
            request.getRoles().forEach(roleName -> {
                Role role = roleRepository.findByName(roleName)
                        .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
                roles.add(role);
            });
        }

        user.setRoles(roles);
        return userRepository.save(user);
    }

    public LoginResult login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String jwt = jwtUtil.generateToken(userDetails);

        // Create refreshToken
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getUser().getId());

        Set<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        // Create AuthResponse without tokens
        AuthResponse authResponse = new AuthResponse(
                userDetails.getUser().getId(),
                userDetails.getUsername(),
                userDetails.getUser().getEmail(),
                roles,
                jwt,                    // Include access token
                refreshToken.getToken() // Include refresh token
        );

        // Return LoginResult with both tokens and user data
        return new LoginResult(jwt, refreshToken.getToken(), authResponse);

    }

}
