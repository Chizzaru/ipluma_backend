package com.github.ws_ncip_pnpki.model;


import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentShared {


    @EmbeddedId
    private DocumentSharedId id = new DocumentSharedId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("documentId")
    @JoinColumn(name = "document_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;


    @Column( nullable = false, columnDefinition = "boolean default false")
    private boolean isDownloadable;


    private String permission;

    private int stepNumber;

    private boolean doneSigning;


    private Instant signedAt;

    @Column(nullable = false, updatable = false)
    private Instant sharedAt;

    public DocumentShared(Document doc, User user, boolean isDownloadable, String permission, int step) {
        this.document = doc;
        this.user = user;
        this.isDownloadable = isDownloadable;
        this.permission = permission;
        this.stepNumber = step;
    }

    @PrePersist
    protected void onInsert(){
        this.doneSigning = false;
        this.signedAt = null;
        this.sharedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate(){
        if(doneSigning && signedAt == null){
            signedAt = Instant.now();
        }
    }





    public DocumentShared(Document document, User user, boolean isDownloadable, String permission) {
        this.document = document;
        this.user = user;
        this.isDownloadable = isDownloadable;
        this.permission = permission;
    }
}
