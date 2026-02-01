package com.github.ws_ncip_pnpki.dto;

import com.github.ws_ncip_pnpki.model.DocumentStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ShareV2Response {


    private Long id;
    private String fileName;
    private String filePath;
    private String fileType;
    private Long fileSize;
    private DocumentStatus status;
    private LocalDateTime uploadedAt;
    private ShareResponse.User ownerDetails;
    private List<ShareResponse.User> sharedToUsers;
    private boolean availableForViewing;
    private boolean availableForSigning;
    private List<ShareResponse.SignerStep> signerSteps;


    @Data
    public static class SignerStep{
        private int step;
        private Long userId;
        private ShareResponse.User user;
        private String permission;
        private boolean hasSigned;
        private boolean parallel;

    }

    @Data
    public static class User{
        private Long id;
        private String username;
        private String email;

        public User(Long id, String username, String email) {
            this.id = id; this.username = username; this.email = email;
        }
    }
}
