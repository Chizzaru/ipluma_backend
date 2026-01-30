package com.github.ws_ncip_pnpki.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "document_share_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentShareRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_by_id", nullable = false)
    private User sharedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_with_id", nullable = false)
    private User sharedWith;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime sharedAt = LocalDateTime.now();

    private LocalDateTime revokedAt;

    @Column(length = 1000)
    private String shareMessage;

    @Builder.Default
    private boolean allowDownload = true;

    @Builder.Default
    private boolean allowForwarding = true;

    private LocalDateTime expiresAt;

    @Builder.Default
    private boolean isSignedDocument = false;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ShareStatus status = ShareStatus.ACTIVE;

    public enum ShareStatus {
        ACTIVE, REVOKED, EXPIRED
    }
}