package com.github.ws_ncip_pnpki.repository;

import com.github.ws_ncip_pnpki.model.Certificate;
import com.github.ws_ncip_pnpki.model.Document;
import com.github.ws_ncip_pnpki.model.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    // Find documents owned by a user
    List<Document> findByOwnerId(Long ownerId);

    Page<Document> findByOwnerId(Long userId, Pageable pageable);

    //check if filename already exist using ownerId
    boolean existsByFileNameAndOwnerId(String fileName, Long ownerId);

    // Find documents shared with user
    @Query("SELECT d FROM Document d JOIN d.sharedWithUsers u WHERE u.id = :userId")
    List<Document> findSharedWithUser(@Param("userId") Long userId);

    // Find all accessible documents for user (owned + shared)
    @Query("SELECT d FROM Document d WHERE d.owner.id = :userId OR EXISTS (SELECT 1 FROM d.sharedWithUsers u WHERE u.id = :userId)")
    List<Document> findAccessibleByUser(@Param("userId") Long userId);

    List<Document> findByStatus(DocumentStatus status);

    // Check if a user has access to a document
    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END FROM Document d WHERE d.id = :documentId AND (d.owner.id = :userId OR EXISTS (SELECT 1 FROM d.sharedWithUsers u WHERE u.id = :userId))")
    boolean existsByIdAndAccessibleByUser(@Param("documentId") Long documentId, @Param("userId") Long userId);


    @Query("""
        SELECT DISTINCT d
        FROM Document d
        LEFT JOIN d.sharedWith sd
        WHERE
            d.owner.id = :userId
            AND d.status IN ('UPLOADED','SIGNED','SHARED','SIGNED_AND_SHARED')
            AND (
            LOWER(d.fileName) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(sd.user.username) LIKE LOWER(CONCAT('%', :search, '%'))
            )
            AND d.deleted = false
       """)
    Page<Document> searchOwnedUploadDocuments(
            @Param("userId") Long userId,
            @Param("search") String search,
            Pageable pageable);

    @Query("""
    SELECT DISTINCT d
    FROM Document d
    LEFT JOIN d.sharedWith sd
    WHERE
        sd.user.id = :userId
        OR (
            d.owner.id != :userId
            AND d.status IN ('SIGNED_AND_SHARED', 'SHARED')
        )
        AND d.deleted = false
""")
    Page<Document> allSharedDocuments(
            @Param("userId") Long userId,
            Pageable pageable
    );

    @Query("""
        SELECT d
        FROM Document d
        JOIN d.sharedWith sd
        WHERE
            d.deleted = false
            AND LOWER(d.owner.username) LIKE LOWER(CONCAT('%', :search, '%'))
    """)
    Page<Document> searchSharedDocuments(
            @Param("userId") Long userId,
            @Param("search") String search,
            Pageable pageable
    );


    @Query("""
       SELECT d FROM Document d
       WHERE d.owner = :userId
         AND d.status = 'SIGNED'
         AND (LOWER(d.fileName) LIKE LOWER(CONCAT('%', :search, '%'))
              OR LOWER(d.fileType) LIKE LOWER(CONCAT('%', :search, '%')))
       """)
    Page<Document> searchSignedDocuments(
            @Param("userId") Long userId,
            @Param("search") String search,
            Pageable pageable);

    Page<Document> findAllByOwnerIdAndStatus(Long userId, DocumentStatus status, Pageable pageable);

    Page<Document> findAllByOwnerIdAndStatusInAndDeletedFalse(
            Long ownerId,
            Collection<DocumentStatus> statuses,
            Pageable pageable
    );





    Page<Document> findAllByOwnerId(Long userId, Pageable pageable);


    Page<Document> findByFileNameContainingOrFileTypeContainingAllIgnoreCase(String fileName, String fileType, Pageable pageable);
    Page<Document> findByOwnerIdAndFileNameContainingOrFileTypeContainingAllIgnoreCase(Long userId, String fileName, String fileType, Pageable pageable);


    @Query("""
        SELECT d FROM Document d
        LEFT JOIN d.sharedWith sw
        WHERE
          (
            d.owner.id = :userId
            AND (
              LOWER(d.fileName) LIKE LOWER(CONCAT('%', :fileName, '%'))
              OR LOWER(d.fileType) LIKE LOWER(CONCAT('%', :fileName, '%'))
            )
          )
          OR sw.user.id = :userId
    """)
    Page<Document> findOwnedAndSharedDocument(
            @Param("userId") Long userId,
            @Param("fileName") String fileName,
            @Param("fileType") String fileType,
            Pageable pageable);


    @Query("""
        SELECT d FROM Document d
        LEFT JOIN d.sharedWith sw
        WHERE
          (
            d.owner.id = :userId
          )
          OR sw.user.id = :userId
    """)
    Page<Document> findOwnedAndSharedDocument(
            @Param("userId") Long userId,
            Pageable pageable);


    // In DocumentRepository
    @Query("SELECT d FROM Document d WHERE d.owner.id = :userId AND d.status IN ('SIGNED', 'SIGNED_AND_SHARED')")
    Page<Document> findAllSignedDocumentsByOwnerId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT d FROM Document d WHERE d.owner.id = :userId AND d.status = 'SIGNED_AND_SHARED'")
    Page<Document> findSignedAndSharedDocumentsByOwnerId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT d FROM Document d WHERE d.owner.id = :userId AND d.status IN ('SHARED', 'SIGNED_AND_SHARED')")
    Page<Document> findSharedDocumentsByOwnerId(@Param("userId") Long userId, Pageable pageable);


    @Query("SELECT d FROM Document d WHERE d.owner.id = :userId AND d.status = 'SIGNED_AND_SHARED'")
    List<Document> findSignedAndSharedDocumentsByOwner(@Param("userId") Long userId);

    @Query("SELECT d FROM Document d JOIN d.sharedWithUsers sw WHERE sw.id = :userId AND d.status = 'SIGNED_AND_SHARED'")
    List<Document> findSharedSignedDocumentsWithUser(@Param("userId") Long userId);

    boolean existsByIdAndStatus(Long id, DocumentStatus status);

    // Find documents by owner and multiple statuses
    List<Document> findByOwnerIdAndStatusIn(Long ownerId, List<DocumentStatus> statuses);

    // Find documents by owner and multiple statuses with pagination
    Page<Document> findByOwnerIdAndStatusIn(Long ownerId, List<DocumentStatus> statuses, Pageable pageable);
    // Find signed and shared documents by owner with pagination
    @Query("SELECT d FROM Document d WHERE d.owner.id = :userId AND d.status = 'SIGNED_AND_SHARED'")
    Page<Document> findSignedAndSharedDocumentsByOwner(@Param("userId") Long userId, Pageable pageable);


    // Check if document is shared with specific user
    @Query("SELECT COUNT(d) > 0 FROM Document d JOIN d.sharedWithUsers sw WHERE d.id = :documentId AND sw.id = :userId")
    boolean existsByIdAndSharedWithUsersId(@Param("documentId") Long documentId, @Param("userId") Long userId);

    // Search methods for signed and shared documents
    @Query("SELECT d FROM Document d WHERE d.owner.id = :userId AND d.status = 'SIGNED_AND_SHARED' AND " +
            "(LOWER(d.fileName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(d.fileType) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Document> searchSignedAndSharedDocuments(@Param("userId") Long userId, @Param("search") String search, Pageable pageable);
}