package com.github.ws_ncip_pnpki.repository;

import com.github.ws_ncip_pnpki.model.Office;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OfficeRepository extends JpaRepository<Office, Long> {
}
