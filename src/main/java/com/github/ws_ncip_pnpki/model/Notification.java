package com.github.ws_ncip_pnpki.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "user_id")
    @JsonIgnore
    private User user;

    @Column
    private String title;

    @Column
    private String message;

    @Column(columnDefinition = "boolean default false")
    private boolean opened;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;


    @PrePersist
    protected void setCreatedAtTimestamp(){
        this.createdAt = Instant.now();
    }


}
