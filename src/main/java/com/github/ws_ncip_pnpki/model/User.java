package com.github.ws_ncip_pnpki.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"ownedDocuments", "sharedDocuments", "receivedForwards"})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;


    // Documents owned by this user
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    @JsonIgnore
    private List<Document> ownedDocuments = new ArrayList<>();

    // Documents shared with this user
    @ManyToMany(mappedBy = "sharedWithUsers", cascade = CascadeType.REMOVE)
    @Builder.Default
    @JsonIgnore
    private List<Document> sharedDocuments = new ArrayList<>();

    // Forwarded documents received by this user
    @OneToMany(mappedBy = "forwardedTo", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @Builder.Default
    @JsonIgnore
    private List<DocumentForward> receivedForwards = new ArrayList<>();

    // Notifications sent by this user (as fromUser)
    @OneToMany(mappedBy = "fromUser", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @Builder.Default
    @JsonIgnore
    private List<Notification> sentNotifications = new ArrayList<>();

    // Notifications received by this user (as toUser)
    @OneToMany(mappedBy = "toUser", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @Builder.Default
    @JsonIgnore
    private List<Notification> receivedNotifications = new ArrayList<>();


    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @JsonIgnore
    private List<DocumentShared> documentSharedList = new ArrayList<>();


    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;



    // Helper methods
    public void addOwnedDocument(Document document) {
        ownedDocuments.add(document);
        document.setOwner(this);
    }

    public List<Document> getAllAccessibleDocuments() {
        List<Document> allDocuments = new ArrayList<>(ownedDocuments);
        allDocuments.addAll(sharedDocuments);
        return allDocuments;
    }



}
