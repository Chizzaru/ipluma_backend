package com.github.ws_ncip_pnpki.model;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"owner","sharedWithUsers","documentForwards"})
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String filePath;

    private String fileType;

    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DocumentStatus status = DocumentStatus.UPLOADED;

    @Column(name = "uploaded_at")
    @Builder.Default
    private LocalDateTime uploadedAt = LocalDateTime.now();

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @Column(name = "shared_at")
    private LocalDateTime sharedAt;

    // Original owner
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToMany
    @JoinTable(
            name = "document_shares",
            joinColumns = @JoinColumn(name = "document_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> sharedWithUsers = new ArrayList<>();

    // One-to-many for tracking forward history
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<DocumentForward> documentForwards = new ArrayList<>();


    @OneToMany(
            mappedBy = "document",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private Set<DocumentShared> sharedWith = new HashSet<>();

    @OneToOne(mappedBy = "document", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private DocumentCheckpoint documentCheckpoint;

    @Column( nullable = false, columnDefinition = "boolean default false")
    private boolean availableForSigning;

    @Column( nullable = false, columnDefinition = "boolean default true")
    private boolean availableForViewing;

    @Column( nullable = false, columnDefinition = "boolean default false")
    private boolean deleted;

    // Business methods
    public void markAsSigned() {
        if (this.status == DocumentStatus.SHARED) {
            this.status = DocumentStatus.SIGNED_AND_SHARED;
        } else {
            this.status = DocumentStatus.SIGNED;
        }
        this.signedAt = LocalDateTime.now();
    }

    public void markAsShared() {
        if (this.status == DocumentStatus.SIGNED) {
            this.status = DocumentStatus.SIGNED_AND_SHARED;
        } else if (this.status == DocumentStatus.UPLOADED) {
            this.status = DocumentStatus.SHARED;
        }
        // For SIGNED_AND_SHARED, no change needed
        this.sharedAt = LocalDateTime.now();
    }

    // Add this method for sharing signed documents
    public void shareSignedDocument(User user) {
        shareWithUser(user);
        if (this.status == DocumentStatus.SIGNED) {
            this.status = DocumentStatus.SIGNED_AND_SHARED;
        }
        if (this.sharedAt == null) {
            this.sharedAt = LocalDateTime.now();
        }
    }
    public void shareWithUser(User user) {
        if (!this.sharedWithUsers.contains(user)) {
            this.sharedWithUsers.add(user);
        }
    }

    public void removeShareWithUser(User user) {
        this.sharedWithUsers.remove(user);
    }

    public boolean isAccessibleBy(User user) {
        return this.owner.equals(user) || this.sharedWithUsers.contains(user);
    }


    public Collection<User> getSharedWith() {
        return this.sharedWithUsers;
    }

    public Set<DocumentShared> getDocumentShared(){
        return this.sharedWith;
    }

    public Boolean openForAccess(){
        return this.getDocumentCheckpoint().getBlockedOthers();
    }

}
