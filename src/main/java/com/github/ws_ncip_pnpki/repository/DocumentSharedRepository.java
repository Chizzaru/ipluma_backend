package com.github.ws_ncip_pnpki.repository;

import com.github.ws_ncip_pnpki.model.DocumentShared;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentSharedRepository extends JpaRepository<DocumentShared, Long> {

    Optional<DocumentShared> findByIdUserIdAndIdDocumentId(Long userId, Long documentId);

    List<DocumentShared> findByIdDocumentId(Long documentId);
}
