package com.github.ws_ncip_pnpki.repository;

import com.github.ws_ncip_pnpki.model.Certificate;
import jakarta.annotation.Nonnull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    List<Certificate> findByUserId(Long userId);

    // Add a paginated version for user-specific certificates
    Page<Certificate> findByUserId(Long userId, Pageable pageable);

    Optional<Certificate> findByUserIdAndCertificateHash(Long userId, String certificateHash);

    boolean existsByUserIdAndCertificateHash(Long userId, String certificateHash);

    Optional<Certificate> findByIdAndUserId(Long id, Long userId);

    void deleteByIdAndUserId(Long id, Long userId);

    // Add methods for all certificates with pagination
    @Nonnull
    @Override
    Page<Certificate> findAll(@Nonnull Pageable pageable);

    // Optional: Count all certificates
    long count();

    Certificate findByUserIdAndIsDefault(Long userId, boolean isDefault);

    Optional<Certificate> findByCertificateHashIgnoreCase(String certificateHash);

    Optional<Certificate> findByUserIdAndIsDefaultTrue(Long userId);


}