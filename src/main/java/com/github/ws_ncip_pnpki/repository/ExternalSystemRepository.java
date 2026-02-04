package com.github.ws_ncip_pnpki.repository;

import com.github.ws_ncip_pnpki.model.ExternalSystem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExternalSystemRepository extends JpaRepository<ExternalSystem, Long> {
}
