package com.github.ws_ncip_pnpki.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class NotificationResponse {

    private Long id;
    private FromUser fromUser;
    private String title;
    private String message;
    private boolean opened;
    private Instant createdAt;


    @Data
    public static class FromUser{
        private Long id;
        private String username;
    }
}
