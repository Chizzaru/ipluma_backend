package com.github.ws_ncip_pnpki.dto;

import com.github.ws_ncip_pnpki.model.DocumentStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PdfUploadResponse {
    private Long id;
    private String fileName;
    private String filePath;
    private String fileType;
    private Long fileSize;
    private DocumentStatus status;
    private LocalDateTime uploadedAt;
    private OwnerDetails ownerDetails;
    private List<UserSearchResponse> sharedToUsers;
    private boolean availableForDownload;
    private String permission;
    private boolean availableForViewing;
    private boolean availableForSigning;




    @Data
    public static class OwnerDetails{
        private Long id;
        private String username;
        private String email;

        public OwnerDetails(Long id, String username, String email) {
            this.id = id; this.username = username; this.email = email;
        }
    }


}
