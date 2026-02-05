package com.github.ws_ncip_pnpki.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "temporary_credentials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TemporaryCredential {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    private String password;
}
