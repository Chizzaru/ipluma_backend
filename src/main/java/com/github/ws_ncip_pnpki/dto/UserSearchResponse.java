package com.github.ws_ncip_pnpki.dto;

import com.github.ws_ncip_pnpki.model.Role;
import lombok.Data;

import java.util.Set;

@Data
public class UserSearchResponse {

    private Long id;
    private String username;
    private String email;
    private Set<Role> roles;

    public UserSearchResponse(Long id, String username, String email, Set<Role> roles) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.roles = roles;
    }
}
