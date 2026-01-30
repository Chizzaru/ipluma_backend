package com.github.ws_ncip_pnpki.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DefaultCertResponse {
    private String certificateHash;
    private LocalDateTime expiresAt;
}
