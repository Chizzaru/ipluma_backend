package com.github.ws_ncip_pnpki.dto;

import com.github.ws_ncip_pnpki.model.SignatureType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignatureResponseDTO {
    private Long id;
    private String fileName;        // Original filename for display
    private String filePath;        // Actual stored path (userId/uuid.png)
    private SignatureType signatureType;
    private boolean isDefault;
    private String previewUrl;
    private String contentType;
    private LocalDateTime createdAt;
    private Long fileSize;
}
