package com.github.ws_ncip_pnpki.service;

import com.github.ws_ncip_pnpki.dto.*;
import com.github.ws_ncip_pnpki.model.*;
import com.github.ws_ncip_pnpki.repository.DocumentForwardRepository;
import com.github.ws_ncip_pnpki.repository.DocumentRepository;
import com.github.ws_ncip_pnpki.repository.DocumentSharedRepository;
import com.github.ws_ncip_pnpki.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class DocumentService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final DocumentForwardRepository documentForwardRepository;
    private final FileStorageService fileStorageService;
    private final DocumentSharedRepository documentSharedRepository;

    @Autowired
    public DocumentService(DocumentRepository documentRepository, UserRepository userRepository, DocumentForwardRepository documentForwardRepository, FileStorageService fileStorageService, DocumentSharedRepository documentSharedRepository) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.documentForwardRepository = documentForwardRepository;
        this.fileStorageService = fileStorageService;
        this.documentSharedRepository = documentSharedRepository;
    }

    @Transactional
    public Document updateDocument(Document doc){
        return documentRepository.save(doc);
    }

    @Transactional
    public Document uploadDocument(MultipartFile file, Long userId) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        String filePath = fileStorageService.storeFile(file, userId, "documents");

        Document document = Document.builder()
                .fileName(file.getOriginalFilename())
                .filePath(filePath)
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .owner(user)
                .availableForSigning(true)
                .build();

        user.addOwnedDocument(document);

        Document savedDocument = documentRepository.save(document);
        log.info("Document uploaded successfully: {}", savedDocument.getFileName());
        return savedDocument;
    }

    @Transactional
    public Document signDocument(Long documentId, Long userId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + documentId));

        // Check if user has permission to sign
        if (!document.getOwner().getId().equals(userId)) {
            throw new RuntimeException("User does not have permission to sign this document");
        }


        document.markAsSigned();

        Document signedDocument = documentRepository.save(document);
        log.info("Document signed: {}", signedDocument.getFileName());
        return signedDocument;
    }

    @Transactional
    public DocumentForward forwardDocument(Long documentId, Long fromUserId, Long toUserId, String message) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + documentId));

        User fromUser = userRepository.findById(fromUserId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + fromUserId));

        User toUser = userRepository.findById(toUserId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + toUserId));

        // Check if fromUser has access to the document
        if (!document.isAccessibleBy(fromUser)) {
            throw new RuntimeException("User does not have access to forward this document");
        }

        // Create a forward record
        DocumentForward forward = DocumentForward.builder()
                .document(document)
                .forwardedBy(fromUser)
                .forwardedTo(toUser)
                .message(message)
                .build();

        // Share document with the target user
        document.shareWithUser(toUser);

        DocumentForward savedForward = documentForwardRepository.save(forward);
        documentRepository.save(document);

        log.info("Document {} forwarded from user {} to user {}",
                document.getFileName(), fromUser.getUsername(), toUser.getUsername());

        return savedForward;
    }



    @Transactional
    public Document shareDocumentWithUser(Long documentId, Long ownerUserId, Long shareWithUserId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + documentId));

        // Check if user is the owner
        if (!document.getOwner().getId().equals(ownerUserId)) {
            throw new RuntimeException("Only document owner can share the document");
        }

        User shareWithUser = userRepository.findById(shareWithUserId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + shareWithUserId));

        // Check if trying to share with owner
        if (shareWithUser.getId().equals(ownerUserId)) {
            throw new RuntimeException("Document is already owned by you");
        }

        // Check if already shared with this user
        if (document.getSharedWith().contains(shareWithUser)) {
            throw new RuntimeException("Document already shared with this user");
        }

        // Share the document
        document.shareWithUser(shareWithUser);

        // Update document status based on current state
        if (document.getStatus() == DocumentStatus.UPLOADED) {
            document.setStatus(DocumentStatus.SHARED);
        } else if (document.getStatus() == DocumentStatus.SIGNED) {
            document.setStatus(DocumentStatus.SIGNED_AND_SHARED);
        }
        // If already SHARED or SIGNED_AND_SHARED, status remains the same

        if (document.getSharedAt() == null) {
            document.setSharedAt(LocalDateTime.now());
        }

        Document sharedDocument = documentRepository.save(document);

        log.info("Document '{}' (ID: {}) with status {} shared by user {} with user {}",
                document.getFileName(), documentId, document.getStatus(), ownerUserId, shareWithUserId);

        return sharedDocument;
    }

    @Transactional
    public DocumentForward shareSignedDocument(Long documentId, Long ownerUserId, Long shareWithUserId, String shareMessage) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + documentId));

        // 1. Verify the requester is the owner
        if (!document.getOwner().getId().equals(ownerUserId)) {
            throw new RuntimeException("Only document owner can share the document");
        }

        // 2. Verify document is signed
        if (document.getStatus() != DocumentStatus.SIGNED &&
                document.getStatus() != DocumentStatus.SIGNED_AND_SHARED) {
            throw new RuntimeException("Document must be signed before sharing. Current status: " +
                    document.getStatus());
        }

        User shareWithUser = userRepository.findById(shareWithUserId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + shareWithUserId));

        // 3. Check if trying to share with owner
        if (shareWithUser.getId().equals(ownerUserId)) {
            throw new RuntimeException("Document is already owned by you");
        }

        // 4. Check if already shared with this user
        if (documentRepository.existsByIdAndSharedWithUsersId(documentId, shareWithUserId)) {
            throw new RuntimeException("Document already shared with this user");
        }

        // 5. Share the signed document
        document.shareWithUser(shareWithUser);

        // 6. Update status to SIGNED_AND_SHARED if it's only SIGNED
        if (document.getStatus() == DocumentStatus.SIGNED) {
            document.setStatus(DocumentStatus.SIGNED_AND_SHARED);
        }

        // 7. Update shared timestamp
        if (document.getSharedAt() == null) {
            document.setSharedAt(LocalDateTime.now());
        }

        // 8. Create a forward record for tracking - mark as signed document
        DocumentForward forward = DocumentForward.builder()
                .document(document)
                .forwardedBy(document.getOwner())
                .forwardedTo(shareWithUser)
                .message(shareMessage != null ? shareMessage : "Signed document shared with you")
                .isSignedDocument(true)  // Mark this as a signed document forward
                .status(ForwardStatus.ACCEPTED)  // Auto-accept signed document shares
                .acceptedAt(LocalDateTime.now())
                .build();

        // 9. Save everything
        documentRepository.save(document);
        DocumentForward savedForward = documentForwardRepository.save(forward);

        log.info("Signed document '{}' (ID: {}) shared by owner {} with user {}. Forward ID: {}",
                document.getFileName(), documentId, ownerUserId, shareWithUserId, savedForward.getId());

        return savedForward;
    }

    // Get all signed document forwards for a user
    public List<DocumentForward> getSignedDocumentForwardsForUser(Long userId) {
        return documentForwardRepository.findByForwardedToIdAndIsSignedDocumentTrue(userId);
    }


    // Get accepted signed document forwards
    public List<DocumentForward> getAcceptedSignedDocumentForwards(Long userId) {
        return documentForwardRepository.findAcceptedSignedDocumentForwards(userId);
    }

    public Document findById(Long documentId){
        return documentRepository.findById(documentId).orElseThrow();
    }

    // When a user open shared document for signing other user should be blocked
    public PdfUploadResponse blockedOthersForSigning(Long documentId, Long userId){
        Document doc = documentRepository.findById(documentId).orElseThrow();
        doc.setAvailableForSigning(false);

        Document saved = documentRepository.save(doc);

        List<UserSearchResponse> sharedToUsers =
                documentSharedRepository.findByIdDocumentId(saved.getId())
                        .stream()
                        .map(DocumentShared::getUser)
                        .map(user -> new UserSearchResponse(
                                user.getId(),
                                user.getUsername(),
                                user.getEmail(),
                                user.getRoles()))
                        .toList();

        DocumentShared shared =
                documentSharedRepository
                        .findByIdUserIdAndIdDocumentId(userId, documentId).orElse(null);

        PdfUploadResponse response = getPdfUploadResponse(saved, sharedToUsers, shared);

        // Broadcast update to all connected user
        messagingTemplate.convertAndSend("/topic/doc-updates", response);

        return response;
    }

    private static PdfUploadResponse getPdfUploadResponse(Document saved, List<UserSearchResponse> sharedToUsers, DocumentShared shared) {
        PdfUploadResponse response = new PdfUploadResponse();
        response.setId(saved.getId());
        response.setFileName(saved.getFileName());
        response.setFilePath(saved.getFilePath());
        response.setFileType(saved.getFileType());
        response.setFileSize(saved.getFileSize());
        response.setStatus(saved.getStatus());
        response.setUploadedAt(saved.getUploadedAt());
        response.setOwnerDetails(new PdfUploadResponse.OwnerDetails(
                saved.getOwner().getId(), saved.getOwner().getUsername(), saved.getOwner().getEmail()));
        response.setSharedToUsers(sharedToUsers);
        response.setAvailableForDownload(shared != null && shared.isDownloadable());
        response.setPermission(shared !=null ? shared.getPermission() : null);
        response.setAvailableForSigning(saved.isAvailableForSigning());
        response.setAvailableForViewing(saved.isAvailableForViewing());
        return response;
    }

    // When a user close shared document that he/she is signing, other user should be unblocked
    public PdfUploadResponse unblockOthersForSigning(Long documentId, Long userId){
        Document doc = documentRepository.findById(documentId).orElseThrow();
        doc.setAvailableForSigning(true);

        Document saved = documentRepository.save(doc);

        List<UserSearchResponse> sharedToUsers =
                documentSharedRepository.findByIdDocumentId(saved.getId())
                        .stream()
                        .map(DocumentShared::getUser)
                        .map(user -> new UserSearchResponse(
                                user.getId(),
                                user.getUsername(),
                                user.getEmail(),
                                user.getRoles()))
                        .toList();

        DocumentShared shared =
                documentSharedRepository
                        .findByIdUserIdAndIdDocumentId(userId, documentId).orElse(null);

        PdfUploadResponse response = getPdfUploadResponse(saved, sharedToUsers, shared);

        // Broadcast update to all connected user
        messagingTemplate.convertAndSend("/topic/doc-updates", response);

        return response;
    }

    // Share signed document with multiple users
    @Transactional
    public List<DocumentForward> shareSignedDocumentWithMultipleUsers(Long documentId, Long ownerUserId,
                                                                      List<Long> shareWithUserIds, String shareMessage) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        if (!document.getOwner().getId().equals(ownerUserId)) {
            throw new RuntimeException("Only document owner can share the document");
        }

        if (document.getStatus() != DocumentStatus.SIGNED &&
                document.getStatus() != DocumentStatus.SIGNED_AND_SHARED) {
            throw new RuntimeException("Document must be signed");
        }

        List<DocumentForward> forwards = new ArrayList<>();
        List<User> usersToShare = userRepository.findAllById(shareWithUserIds);

        for (User user : usersToShare) {
            if (!user.getId().equals(ownerUserId) &&
                    !documentRepository.existsByIdAndSharedWithUsersId(documentId, user.getId())) {

                // Share with user
                document.shareWithUser(user);

                // Create forward record
                DocumentForward forward = DocumentForward.builder()
                        .document(document)
                        .forwardedBy(document.getOwner())
                        .forwardedTo(user)
                        .message(shareMessage)
                        .isSignedDocument(true)
                        .status(ForwardStatus.ACCEPTED)
                        .acceptedAt(LocalDateTime.now())
                        .build();

                forwards.add(forward);
            }
        }

        // Update document status if needed
        if (document.getStatus() == DocumentStatus.SIGNED) {
            document.setStatus(DocumentStatus.SIGNED_AND_SHARED);
        }

        if (document.getSharedAt() == null) {
            document.setSharedAt(LocalDateTime.now());
        }

        documentRepository.save(document);
        documentForwardRepository.saveAll(forwards);

        log.info("Signed document '{}' shared with {} users", document.getFileName(), forwards.size());
        return forwards;
    }


    /**@Transactional
    public Document unshareDocument(Long documentId, Long ownerUserId, Long unshareWithUserId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + documentId));

        // Check if user is the owner
        if (!document.getOwner().getId().equals(ownerUserId)) {
            throw new RuntimeException("Only document owner can unshare the document");
        }

        User unshareWithUser = userRepository.findById(unshareWithUserId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + unshareWithUserId));

        // Check if document is actually shared with this user
        if (!document.getSharedWith().contains(unshareWithUser)) {
            throw new RuntimeException("Document is not shared with this user");
        }

        // Remove the share
        document.removeShareWithUser(unshareWithUser);

        // Remove user from their shared documents list
        unshareWithUser.getSharedDocuments().remove(document);

        // Remove related forwards to this user
        documentForwardRepository.deleteByDocumentIdAndForwardedToId(documentId, unshareWithUserId);

        // Update status if no more shared users
        if (document.getSharedWith().isEmpty()) {
            if (document.getStatus() == DocumentStatus.SHARED) {
                document.setStatus(DocumentStatus.UPLOADED);
            } else if (document.getStatus() == DocumentStatus.SIGNED_AND_SHARED) {
                document.setStatus(DocumentStatus.SIGNED);
            }
            document.setSharedAt(null);
        }

        Document updatedDocument = documentRepository.save(document);

        log.info("Document '{}' (ID: {}) unshared with user {}",
                document.getFileName(), documentId, unshareWithUserId);

        return updatedDocument;
    }*/

    @Transactional
    public Document unshareDocument(Long documentId, List<Long> userIds) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        List<User> usersToRemove = userRepository.findAllById(userIds);

        document.getSharedWithUsers().removeAll(usersToRemove);

        return documentRepository.save(document);
    }

    public boolean alreadyExist(String fileName, Long ownerId){
        return documentRepository.existsByFileNameAndOwnerId(fileName, ownerId);
    }

    @Transactional
    public void updateDelete(Long documentId){
        Document doc = documentRepository.findById(documentId).orElseThrow();
        doc.setDeleted(true);
        documentRepository.save(doc);
    }


    @Transactional
    public void deleteDocument(Long documentId, Long userId) {

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + documentId));

        // Only the owner can delete the document
        if (!document.getOwner().getId().equals(userId)) {
            throw new RuntimeException("You do not have permission to delete this document.");
        }

        // Delete a file from storage
        try {
            fileStorageService.deleteFile(document.getFilePath());
        } catch (Exception ex) {
            log.error("Failed to delete file from storage: {}", ex.getMessage());
            throw new RuntimeException("Failed to delete the file from storage.");
        }

        // Delete all forwarding records related to this document
        List<DocumentForward> forwards = documentForwardRepository.findByDocumentId(documentId);
        documentForwardRepository.deleteAll(forwards);

        // Remove associations with shared users
        document.getSharedWith().clear();

        // Delete the document record
        documentRepository.delete(document);

        log.info("Document deleted successfully: {}", document.getFileName());
    }


    public List<Document> getAccessibleDocuments(Long userId) {
        return documentRepository.findAccessibleByUser(userId);
    }

    public String saveSignedDocument(MultipartFile file, Long userId) throws IOException {
        return fileStorageService.storeFile(file, userId, "signed");
    }


    public Page<ShareResponse> getOwnedUploadDocuments(Long userId, int page, int limit, int offset, String sortBy, String sortDirection, String search){

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);

        Pageable pageable;

        if(offset > 0){
            pageable = new OffsetBasedPageRequest(offset, limit, sort);
        } else {
            // Fallback to regular page-based
            int actualPage = Math.max(0, page - 1);
            pageable = PageRequest.of(actualPage, limit, sort);
        }


        // Execute a query with search
        Page<Document> documentPage;
        if (search != null && !search.trim().isEmpty()) {
            documentPage = documentRepository.searchOwnedUploadDocuments(userId, search, pageable);
        } else {
            //documentPage = documentRepository.findAllByOwnerIdAndStatus( userId, DocumentStatus.UPLOADED, pageable);
            documentPage = documentRepository.findAllByOwnerIdAndStatusInAndDeletedFalse(
                    userId,
                    List.of(DocumentStatus.UPLOADED,
                            DocumentStatus.SIGNED ,
                            DocumentStatus.SHARED,
                            DocumentStatus.SIGNED_AND_SHARED),
                    pageable
            );
        }

        return documentPage.map(document -> convertToShareResponse(document, userId));
    }

    public Page<PdfUploadResponse> getSharedDocuments(Long userId, int page, int limit, int offset, String sortBy, String sortDirection, String search){
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);

        Pageable pageable;

        if(offset > 0){
            pageable = new OffsetBasedPageRequest(offset, limit, sort);
        }else {
            // Fallback to regular page-based
            int actualPage = Math.max(0, page - 1);
            pageable = PageRequest.of(actualPage, limit, sort);
        }

        // Execute a query with search
        Page<Document> documentPage;
        if(search != null && !search.trim().isEmpty()){
            documentPage = documentRepository.searchSharedDocuments(userId, search, pageable);
        } else {
            documentPage = documentRepository.allSharedDocuments(userId, pageable);
        }

        return documentPage.map(document -> convertToResponse(document, userId));
    }

    public List<Document> getOwnedDocuments(Long userId) {
        return documentRepository.findByOwnerId(userId);
    }

    public Page<ShareResponse> getOwnedDocuments(Long userId, int page, int limit, int offset, String sortBy, String sortDirection, String search){

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);

        Pageable pageable;

        if(offset > 0){
            pageable = new OffsetBasedPageRequest(offset, limit, sort);
        } else {
            // Fallback to regular page-based
            int actualPage = Math.max(0, page - 1);
            pageable = PageRequest.of(actualPage, limit, sort);
        }


        // Execute a query with search
        Page<Document> documentPage;
        if (search != null && !search.trim().isEmpty()) {
            //documentPage = documentRepository.findByOwnerIdAndFileNameContainingOrFileTypeContainingAllIgnoreCase(userId, search, search, pageable);
            documentPage = documentRepository.findOwnedAndSharedDocument(userId, search, search, pageable);

        } else {
            documentPage = documentRepository.findOwnedAndSharedDocument(userId, pageable);
        }

        return documentPage.map(document -> convertToShareResponse(document, userId));
    }

    public Page<PdfUploadResponse> getOwnedSignedDocuments(Long userId, int page, int limit, int offset, String sortBy, String sortDirection, String search){

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);

        Pageable pageable;

        if(offset > 0){
            pageable = new OffsetBasedPageRequest(offset, limit, sort);
        } else {
            // Fallback to regular page-based
            int actualPage = Math.max(0, page - 1);
            pageable = PageRequest.of(actualPage, limit, sort);
        }


        // Execute a query with search
        Page<Document> documentPage;
        if (search != null && !search.trim().isEmpty()) {
            documentPage = documentRepository.searchSignedDocuments(userId, search, pageable);
        } else {
            documentPage = documentRepository.findAllByOwnerIdAndStatus( userId,DocumentStatus.SIGNED , pageable);
        }

        return documentPage.map(document -> convertToResponse(document, userId));
    }

    public List<Document> getSharedDocuments(Long userId) {
        return documentRepository.findSharedWithUser(userId);
    }

    public List<DocumentForward> getReceivedForwards(Long userId) {
        return documentForwardRepository.findByForwardedToId(userId);
    }

    public List<DocumentForward> getSentForwards(Long userId) {
        return documentForwardRepository.findByForwardedById(userId);
    }

    public boolean hasDocumentAccess(Long documentId, Long userId) {
        return documentRepository.existsByIdAndAccessibleByUser(documentId, userId);
    }

    public long countOwnedDocuments(Long userId){
        return documentRepository.findByOwnerId(userId).size();
    }


    // Update deleted to true
    public void setDeleted(Long documentId, boolean deleted){
        Document doc = documentRepository.findById(documentId).orElseThrow();
        doc.setDeleted(deleted);
        documentRepository.save(doc);
    }

    public long countDocuments(){
        return documentRepository.count();
    }

    public Document getDocument(Long documentId){
        return documentRepository.findById(documentId).orElseThrow(() -> new IllegalArgumentException("Document not found"));
    }

    private ShareResponse convertToShareResponse(Document document, Long userId) {

        List<DocumentShared> ds = documentSharedRepository.findByIdDocumentId(document.getId());

        DocumentShared shared =
                documentSharedRepository
                        .findByIdUserIdAndIdDocumentId(userId, document.getId())
                        .orElse(null);

        List<ShareResponse.User> sharedToUsers =
                documentSharedRepository.findByIdDocumentId(document.getId())
                        .stream()
                        .map(DocumentShared::getUser)
                        .map(user -> new ShareResponse.User(
                                user.getId(),
                                user.getUsername(),
                                user.getEmail()))
                        .toList();

        return getShareResponse(document, sharedToUsers, shared, ds);
    }


    private PdfUploadResponse convertToResponse(Document document, Long userId) {

        DocumentShared shared =
                documentSharedRepository
                        .findByIdUserIdAndIdDocumentId(userId, document.getId())
                        .orElse(null);

        List<UserSearchResponse> sharedToUsers =
                documentSharedRepository.findByIdDocumentId(document.getId())
                        .stream()
                        .map(DocumentShared::getUser)
                        .map(user -> new UserSearchResponse(
                                user.getId(),
                                user.getUsername(),
                                user.getEmail(),
                                user.getRoles()))
                        .toList();

        return getUploadResponse(document, sharedToUsers, shared);
    }

    private static PdfUploadResponse getUploadResponse(Document document, List<UserSearchResponse> sharedToUsers, DocumentShared shared) {
        PdfUploadResponse response = new PdfUploadResponse();
        response.setId(document.getId());
        response.setFileName(document.getFileName());
        response.setFilePath(document.getFilePath());
        response.setFileType(document.getFileType());
        response.setFileSize(document.getFileSize());
        response.setStatus(document.getStatus());
        response.setUploadedAt(document.getUploadedAt());
        response.setOwnerDetails(new PdfUploadResponse.OwnerDetails(document.getOwner().getId(), document.getOwner().getUsername(), document.getOwner().getEmail()));


        response.setSharedToUsers(sharedToUsers);
        response.setAvailableForDownload(shared != null && shared.isDownloadable());
        response.setPermission(shared != null ? shared.getPermission() : null);
        response.setAvailableForViewing(document.isAvailableForViewing());
        response.setAvailableForSigning(document.isAvailableForSigning());
        response.setParallel(shared != null && shared.isParallel());
        return response;
    }



    private static ShareResponse getShareResponse(Document document, List<ShareResponse.User> sharedToUsers, DocumentShared shared, List<DocumentShared> ds) {
        ShareResponse response = new ShareResponse();
        response.setId(document.getId());
        response.setFileName(document.getFileName());
        response.setFilePath(document.getFilePath());
        response.setFileType(document.getFileType());
        response.setFileSize(document.getFileSize());
        response.setStatus(document.getStatus());
        response.setUploadedAt(document.getUploadedAt());
        response.setOwnerDetails(new ShareResponse.User(document.getOwner().getId(), document.getOwner().getUsername(), document.getOwner().getEmail()));


        response.setSharedToUsers(sharedToUsers);
        response.setAvailableForViewing(document.isAvailableForViewing());
        response.setAvailableForSigning(document.isAvailableForSigning());

        List<ShareResponse.SignerStep> signerSteps = ds.stream()
                        .map(dShared-> {
                            ShareResponse.User su = new ShareResponse.User(
                                    dShared.getUser().getId(),
                                    dShared.getUser().getUsername(),
                                    dShared.getUser().getEmail()
                            );

                            ShareResponse.SignerStep step = new ShareResponse.SignerStep();
                            step.setStep(dShared.getStepNumber());
                            step.setUserId(dShared.getUser().getId());
                            step.setUser(su);
                            step.setPermission(dShared.getPermission());
                            step.setHasSigned(dShared.getSignedAt() != null);
                            step.setParallel(dShared.isParallel());
                            return step;
                        }).toList();
        response.setSignerSteps(signerSteps);
        return response;
    }



    public List<Document> getSignedAndSharedDocuments(Long userId) {
        return documentRepository.findByOwnerIdAndStatusIn(
                userId,
                List.of(DocumentStatus.SIGNED_AND_SHARED));
    }

    public boolean isDocumentSignedAndShared(Long documentId) {
        return documentRepository.existsByIdAndStatus(documentId, DocumentStatus.SIGNED_AND_SHARED);
    }

    public List<Document> getSharedSignedDocumentsWithUser(Long userId) {
        // Get all signed documents shared with this user
        return documentRepository.findSharedSignedDocumentsWithUser(userId);
    }


    // Get pending forwards for a user (both signed and unsigned)
    public List<DocumentForward> getPendingForwards(Long userId) {
        return documentForwardRepository.findByForwardedToIdAndStatus(userId, ForwardStatus.PENDING);
    }

    // Get accepted forwards for a user
    public List<DocumentForward> getAcceptedForwards(Long userId) {
        return documentForwardRepository.findByForwardedToIdAndStatus(userId, ForwardStatus.ACCEPTED);
    }

    // Get rejected forwards for a user
    public List<DocumentForward> getRejectedForwards(Long userId) {
        return documentForwardRepository.findByForwardedToIdAndStatus(userId, ForwardStatus.REJECTED);
    }

    // Get pending signed document forwards
    public List<DocumentForward> getPendingSignedDocumentForwards(Long userId) {
        return documentForwardRepository.findByForwardedToIdAndStatusAndIsSignedDocumentTrue(
                userId, ForwardStatus.PENDING);
    }

    // Get all signed document forwards with a specific status
    public List<DocumentForward> getSignedDocumentForwardsByStatus(Long userId, ForwardStatus status) {
        return documentForwardRepository.findByForwardedToIdAndStatusAndIsSignedDocumentTrue(userId, status);
    }

    // Get all forwards for a user by multiple statuses
    public List<DocumentForward> getForwardsByStatuses(Long userId, List<ForwardStatus> statuses) {
        return documentForwardRepository.findByForwardedToIdAndStatusIn(userId, statuses);
    }

    // Count pending forwards for a user
    public long countPendingForwards(Long userId) {
        return documentForwardRepository.countByForwardedToIdAndStatus(userId, ForwardStatus.PENDING);
    }

    // Count all forwards for a user
    public long countAllForwardsForUser(Long userId) {
        return documentForwardRepository.countByForwardedToId(userId);
    }

    @Transactional
    public DocumentForward acceptForward(Long forwardId, Long userId) {
        DocumentForward forward = documentForwardRepository.findById(forwardId)
                .orElseThrow(() -> new RuntimeException("Forward not found with id: " + forwardId));

        // Check if user is the recipient
        if (!forward.getForwardedTo().getId().equals(userId)) {
            throw new RuntimeException("User cannot accept this forward");
        }

        // Check if already processed
        if (forward.getStatus() != ForwardStatus.PENDING) {
            throw new RuntimeException("Forward is already " + forward.getStatus());
        }

        // For signed documents, auto-accept might have already happened
        if (forward.isSignedDocument() && forward.getStatus() == ForwardStatus.ACCEPTED) {
            log.warn("Signed document forward {} was already auto-accepted", forwardId);
            return forward;
        }

        forward.accept(); // Uses the helper method in DocumentForward entity
        return documentForwardRepository.save(forward);
    }

    @Transactional
    public DocumentForward rejectForward(Long forwardId, Long userId) {
        DocumentForward forward = documentForwardRepository.findById(forwardId)
                .orElseThrow(() -> new RuntimeException("Forward not found"));

        if (!forward.getForwardedTo().getId().equals(userId)) {
            throw new RuntimeException("User cannot reject this forward");
        }

        if (forward.getStatus() != ForwardStatus.PENDING) {
            throw new RuntimeException("Forward is already " + forward.getStatus());
        }

        forward.reject(); // Uses the helper method in DocumentForward entity
        return documentForwardRepository.save(forward);
    }

    @Transactional
    public DocumentForward revokeForward(Long forwardId, Long userId) {
        DocumentForward forward = documentForwardRepository.findById(forwardId)
                .orElseThrow(() -> new RuntimeException("Forward not found"));

        // Check if user is the sender
        if (!forward.getForwardedBy().getId().equals(userId)) {
            throw new RuntimeException("Only the sender can revoke a forward");
        }

        forward.setStatus(ForwardStatus.REVOKED);

        // If it's a signed document forward, also unshare the document
        if (forward.isSignedDocument()) {
            Document document = forward.getDocument();
            User recipient = forward.getForwardedTo();

            // Remove the share
            document.removeShareWithUser(recipient);
            documentRepository.save(document);

            log.info("Revoked signed document forward and unshared document {} with user {}",
                    document.getId(), recipient.getId());
        }

        return documentForwardRepository.save(forward);
    }


}
