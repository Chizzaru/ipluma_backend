package com.github.ws_ncip_pnpki.repository;

import com.github.ws_ncip_pnpki.model.DocumentForward;
import com.github.ws_ncip_pnpki.model.ForwardStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface DocumentForwardRepository extends JpaRepository<DocumentForward, Long> {

    // Existing methods
    List<DocumentForward> findByDocumentId(Long documentId);
    List<DocumentForward> findByForwardedById(Long forwardedById);
    List<DocumentForward> findByForwardedToId(Long forwardedToId);

    // Add these missing methods:
    List<DocumentForward> findByForwardedToIdAndStatus(Long forwardedToId, ForwardStatus status);
    List<DocumentForward> findByForwardedByIdAndStatus(Long forwardedById, ForwardStatus status);
    List<DocumentForward> findByDocumentIdAndStatus(Long documentId, ForwardStatus status);

    // Find by multiple statuses
    List<DocumentForward> findByForwardedToIdAndStatusIn(Long forwardedToId, List<ForwardStatus> statuses);

    // Signed document specific queries
    List<DocumentForward> findByIsSignedDocumentTrue();
    List<DocumentForward> findByIsSignedDocumentFalse();
    List<DocumentForward> findByForwardedToIdAndIsSignedDocumentTrue(Long forwardedToId);
    List<DocumentForward> findByForwardedToIdAndIsSignedDocumentFalse(Long forwardedToId);
    List<DocumentForward> findByDocumentIdAndIsSignedDocumentTrue(Long documentId);
    List<DocumentForward> findByDocumentIdAndIsSignedDocumentFalse(Long documentId);
    List<DocumentForward> findByStatusAndIsSignedDocumentTrue(ForwardStatus status);
    List<DocumentForward> findByStatusAndIsSignedDocumentFalse(ForwardStatus status);

    // Combined queries
    List<DocumentForward> findByForwardedToIdAndStatusAndIsSignedDocumentTrue(Long forwardedToId, ForwardStatus status);
    List<DocumentForward> findByForwardedToIdAndStatusAndIsSignedDocumentFalse(Long forwardedToId, ForwardStatus status);

    // Custom query methods
    @Query("SELECT df FROM DocumentForward df WHERE df.document.id = :documentId AND df.forwardedTo.id = :userId")
    Optional<DocumentForward> findByDocumentAndForwardedTo(@Param("documentId") Long documentId, @Param("userId") Long userId);

    @Query("SELECT df FROM DocumentForward df WHERE df.forwardedTo.id = :userId AND df.isSignedDocument = true AND df.status = 'ACCEPTED'")
    List<DocumentForward> findAcceptedSignedDocumentForwards(@Param("userId") Long userId);

    @Query("SELECT COUNT(df) FROM DocumentForward df WHERE df.document.id = :documentId AND df.isSignedDocument = true")
    long countSignedDocumentForwards(@Param("documentId") Long documentId);

    // Delete methods
    @Modifying
    @Transactional
    @Query("DELETE FROM DocumentForward df WHERE df.document.id = :documentId AND df.forwardedTo.id = :userId")
    void deleteByDocumentIdAndForwardedToId(@Param("documentId") Long documentId, @Param("userId") Long userId);

    // Derived delete methods (optional)
    @Modifying
    @Transactional
    void deleteByDocumentId(Long documentId);

    @Modifying
    @Transactional
    void deleteByForwardedToId(Long forwardedToId);

    // Find by status
    List<DocumentForward> findByStatus(ForwardStatus status);

    // Count methods
    long countByForwardedToId(Long forwardedToId);
    long countByForwardedToIdAndStatus(Long forwardedToId, ForwardStatus status);
    long countByDocumentId(Long documentId);
    long countByForwardedById(Long forwardedById);

    // Find with pagination (if needed later)
    // Page<DocumentForward> findByForwardedToId(Long forwardedToId, Pageable pageable);
    // Page<DocumentForward> findByForwardedToIdAndStatus(Long forwardedToId, ForwardStatus status, Pageable pageable);
}