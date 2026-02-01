package com.github.ws_ncip_pnpki.dto;

import lombok.Data;

import java.util.List;

@Data
public class UnshareRequest {

    private Long currentUserId;
    private Long documentId;
    private List<Long> userIds;
}
