package com.github.ws_ncip_pnpki.dto;

import lombok.Data;

@Data
public class ForwardRequest {
    private Long fromUserId;
    private Long toUserId;
    private String message;
}
