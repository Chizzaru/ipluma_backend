package com.github.ws_ncip_pnpki.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "certificates", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "certificate_hash"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String storedFileName;

    @Column(nullable = false, unique = true)
    private String certificateHash;

    @Column(nullable = false)
    private String filePath;

    private Long fileSize;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    private LocalDateTime expiresAt;

    @Column(length = 500)
    private String issuer;

    @Column(length = 500)
    private String subject;

    @Column(columnDefinition = "boolean default false")
    private boolean isDefault;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }
}