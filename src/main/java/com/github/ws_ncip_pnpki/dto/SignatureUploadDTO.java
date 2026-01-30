package com.github.ws_ncip_pnpki.dto;

import com.github.ws_ncip_pnpki.model.SignatureType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignatureUploadDTO {
    private SignatureType signatureType;
}
