package com.github.ws_ncip_pnpki.dto;

import com.github.ws_ncip_pnpki.validation.ValidFileType;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import javax.validation.constraints.Size;

@Data
public class SetupRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    private String newPassword;

    @NotNull(message = "Certificate is required")
    @ValidFileType(
            allowedExtensions = {"p12","pfx"},
            allowedMimeTypes = {
                    "application/x-pkcs12",
                    "application/pkcs12",
                    "application/octet-stream"  // Some p12 files may have this
            },
            message = "Certificate must be a .p12 or .pfx file"
    )
    private MultipartFile certificate;

    @NotNull(message = "Password is required")
    private String certPassword;

    @ValidFileType(
            allowedExtensions = {"png"},
            allowedMimeTypes = {"image/png"},
            required = false,
            message = "Full signature must be a .png file"
    )
    private MultipartFile fullSignature;

    @ValidFileType(
            allowedExtensions = {"png"},
            allowedMimeTypes = {"image/png"},
            required = false,
            message = "Initial signature must be a .png file"
    )
    private MultipartFile initialSignature;
}
