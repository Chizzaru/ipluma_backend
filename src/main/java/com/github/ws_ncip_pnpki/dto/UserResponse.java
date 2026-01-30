package com.github.ws_ncip_pnpki.dto;

import com.github.ws_ncip_pnpki.model.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Builder
@Data
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private Set<Role> role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
