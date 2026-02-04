package com.github.ws_ncip_pnpki.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class AddExternalSystemResponse {
    public Long id;
    public String applicationName;
    public String applicationUrl;
    public String secretKey;
    public Instant createdAt;
}
