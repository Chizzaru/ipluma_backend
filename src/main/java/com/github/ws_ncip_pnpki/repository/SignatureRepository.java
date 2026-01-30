package com.github.ws_ncip_pnpki.repository;

import com.github.ws_ncip_pnpki.model.Signature;
import com.github.ws_ncip_pnpki.model.SignatureType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SignatureRepository extends JpaRepository<Signature, Long> {
    List<Signature> findByUserId(Long userId);

    List<Signature> findByUserIdAndSignatureType(Long userId, SignatureType signatureType);

    List<Signature> findBySignatureType(SignatureType signatureType);

    List<Signature> findByUserIdAndIsDefault(Long userId, boolean isDefault);

    Optional<Signature> findByUserIdAndSignatureTypeAndIsDefault(Long userId, SignatureType signatureType, boolean isDefault);

    Optional<Signature> findByIdAndUserId(Long id, Long userId);

    void deleteByIdAndUserId(Long id, Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);
}
