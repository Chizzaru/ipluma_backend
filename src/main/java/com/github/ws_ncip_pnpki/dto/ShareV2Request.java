package com.github.ws_ncip_pnpki.dto;

import lombok.Data;

import java.util.List;

@Data
public class ShareV2Request {

    private Long documentId;
    private boolean downloadable;
    private String message;
    private List<User> users;

    @Data
    public static class User{
        private Long userId;
        private int step;
        private boolean parallel;
        private String permission;
    }
}
