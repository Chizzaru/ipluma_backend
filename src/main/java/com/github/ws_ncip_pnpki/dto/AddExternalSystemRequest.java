package com.github.ws_ncip_pnpki.dto;

import lombok.Data;

@Data
public class AddExternalSystemRequest {
    public String applicationName;
    public String applicationUrl;
}
