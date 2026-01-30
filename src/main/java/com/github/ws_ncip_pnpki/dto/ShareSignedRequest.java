package com.github.ws_ncip_pnpki.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ShareSignedRequest {
    private Long ownerUserId;
    private Long shareWithUserId;
    private String shareMessage;
    // Optional fields if you want to support them:
    private boolean allowDownload = true;
    private boolean allowForwarding = true;
    private LocalDateTime expiresAt;
}