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

    // If step 1 has 3 users, you’ll get all 3 user IDs in one call.
    @Query("""
    select ds.id.userId
    from DocumentShared ds
    where ds.id.documentId = :documentId
      and ds.signedAt is null
      and ds.permission = 'view_and_sign'
      and ds.stepNumber = (
          select min(ds2.stepNumber)
          from DocumentShared ds2
          where ds2.id.documentId = :documentId
            and ds2.signedAt is null
            and ds2.permission = 'view_and_sign'
      )
""")
    List<Long> findNextUserIdsToSign(@Param("documentId") Long documentId);

    void deleteByDocumentIdAndUserId(Long documentId, Long userId);
}
