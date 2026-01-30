package com.github.ws_ncip_pnpki.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "document_forwards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"document", "forwardedBy", "forwardedTo"})
public class DocumentForward {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forwarded_by", nullable = false)
    private User forwardedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forwarded_to", nullable = false)
    private User forwardedTo;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime forwardedAt = LocalDateTime.now();

    private String message; // Optional message when forwarding

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ForwardStatus status = ForwardStatus.PENDING;

    @Column(name = "is_signed_document")
    @Builder.Default
    private boolean isSignedDocument = false; // New field to track if this is a signed document forward

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt; // Optional expiration for the forward

    // Helper method to check if forward is expired
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    // Helper method to accept the forward
    public void accept() {
        this.status = ForwardStatus.ACCEPTED;
        this.acceptedAt = LocalDateTime.now();
    }

    // Helper method to reject the forward
    public void reject() {
        this.status = ForwardStatus.REJECTED;
    }
}