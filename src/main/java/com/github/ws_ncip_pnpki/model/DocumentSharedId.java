package com.github.ws_ncip_pnpki.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSharedId implements Serializable {
    private Long documentId;
    private Long userId;
}
