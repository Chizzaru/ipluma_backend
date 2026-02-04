package com.github.ws_ncip_pnpki.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ExternalSystem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String appName;

    @Column(unique = true)
    private String appUrl;

    @Column(unique = true)
    private String secretKey;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant updatedAt;


    @PrePersist
    private void onCreate(){
        this.createdAt = Instant.now();
    }

    @PreUpdate
    private void onUpdate(){
        this.updatedAt = Instant.now();
    }
}
