package com.github.ws_ncip_pnpki.dto;

import lombok.Data;

@Data
public class ShareRequest {
    private Long ownerUserId;
    private Long shareWithUserId;
}
