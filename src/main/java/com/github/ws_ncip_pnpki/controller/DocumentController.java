package com.github.ws_ncip_pnpki.controller;

import com.github.ws_ncip_pnpki.dto.*;
import com.github.ws_ncip_pnpki.model.*;
import com.github.ws_ncip_pnpki.repository.DocumentForwardRepository;
import com.github.ws_ncip_pnpki.service.DocumentService;
import com.github.ws_ncip_pnpki.service.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/documents")
@Slf4j
public class DocumentController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private final DocumentService documentService;
    private final FileStorageService fileStorageService;
    @Autowired
    private DocumentForwardRepository documentForwardRepository;
    @Autowired
    public DocumentController(DocumentService documentService, FileStorageService fileStorageService) {
        this.documentService = documentService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public ResponseEntity<?> getAllDocuments(
            @RequestParam("user_id") Long userId,
            @RequestParam("user_roles") List<String> userRoles,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(required = false) String search
    ){
        try {
            Page<PdfUploadResponse> documentsPage;
            long totalCount;
            // In a real application, you should verify if the user has admin privileges

            /**if(userRoles.contains("ROLE_ADMIN")) {
                documentsPage = documentService.getAllDocuments(page, limit, offset, sortBy, sortDirection, search);
                totalCount = documentService.countDocuments();
            }else{
                documentsPage = documentService.getOwnedDocuments(userId, page, limit, offset,sortBy, sortDirection, search);
                totalCount = documentService.countOwnedDocuments(userId);
            }*/

            documentsPage = documentService.getOwnedDocuments(userId, page, limit, offset,sortBy, sortDirection, search);
            totalCount = documentService.countOwnedDocuments(userId);


            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", documentsPage.getContent());
            response.put("pagination", createPaginationInfo(documentsPage, page, limit, offset, totalCount));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching all certificates", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to fetch certificates"));
        }
    }

    @PostMapping("/locked-in")
    public ResponseEntity<PdfUploadResponse>  lockedIn(
            @RequestBody WsDocUpdateRequest request
    ){
        PdfUploadResponse response = documentService.blockedOthersForSigning(request.getDocumentId(), request.getDocumentId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/locked-out")
    public ResponseEntity<PdfUploadResponse>  lockedOut(
            @RequestBody WsDocUpdateRequest request
    ){
        PdfUploadResponse response = documentService.unblockOthersForSigning(request.getDocumentId(), request.getDocumentId());



        return ResponseEntity.ok(response);
    }

    @GetMapping("/uploaded")
    public ResponseEntity<?> getOwnedUploadDocument(
            @RequestParam("user_id") Long userId,
            @RequestParam("user_roles") List<String> userRoles,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(value = "search", required = false) String search
    ){

        try {
            Page<PdfUploadResponse> documentsPage;
            long totalCount;

            documentsPage = documentService.getOwnedUploadDocuments(userId, page, limit, offset,sortBy, sortDirection, search);
            //totalCount = documentService.countOwnedDocuments(userId);
            totalCount = documentsPage.getTotalElements();


            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", documentsPage.getContent());
            response.put("pagination", createPaginationInfo(documentsPage, page, limit, offset, totalCount));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching all certificates", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to fetch certificates"));
        }

    }

    @GetMapping("/shared")
    public ResponseEntity<?> getSharedDocument(
            @RequestParam("user_id") Long userId,
            @RequestParam("user_roles") List<String> userRoles,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(required = false) String search
    ){
        try{
            Page<PdfUploadResponse> documentPage;
            long totalCount;

            // get shared and shared to me
            documentPage = documentService.getSharedDocuments(userId, page, limit, offset, sortBy, sortDirection, search);
            totalCount = documentPage.getTotalElements();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", documentPage.getContent());
            response.put("pagination", createPaginationInfo(documentPage, page, limit, offset, totalCount));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching all shared documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to fetch shared documents"));
        }
    }

    @GetMapping("/signed")
    public ResponseEntity<?> getSignedDocument(
            @RequestParam("user_id") Long userId,
            @RequestParam("user_roles") List<String> userRoles,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(required = false) String search
    ){

        try {
            Page<PdfUploadResponse> documentsPage;
            long totalCount;
            // In a real application, you should verify if the user has admin privileges

            documentsPage = documentService.getOwnedSignedDocuments(userId, page, limit, offset,sortBy, sortDirection, search);
            totalCount = documentService.countOwnedDocuments(userId);


            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", documentsPage.getContent());
            response.put("pagination", createPaginationInfo(documentsPage, page, limit, offset, totalCount));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching all certificates", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to fetch certificates"));
        }

    }

    @PostMapping("/upload/{userId}")
    public ResponseEntity<PdfUploadResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @PathVariable Long userId) {
        try {
            Document document = documentService.uploadDocument(file, userId);
            PdfUploadResponse response = new PdfUploadResponse();
            response.setId(document.getId());
            response.setFileName(document.getFileName());
            response.setFilePath(document.getFilePath());
            response.setFileType(document.getFileType());
            response.setFileSize(document.getFileSize());
            response.setStatus(document.getStatus());
            response.setUploadedAt(document.getUploadedAt());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{documentId}/sign/{userId}")
    public ResponseEntity<Document> signDocument(
            @PathVariable Long documentId,
            @PathVariable Long userId) {
        try {
            Document document = documentService.signDocument(documentId, userId);
            return ResponseEntity.ok(document);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{documentId}/forward")
    public ResponseEntity<DocumentForward> forwardDocument(
            @PathVariable Long documentId,
            @RequestBody ForwardRequest request) {
        try {
            DocumentForward forward = documentService.forwardDocument(
                    documentId, request.getFromUserId(), request.getToUserId(), request.getMessage());
            return ResponseEntity.ok(forward);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{documentId}/share")
    public ResponseEntity<Document> shareDocument(
            @PathVariable Long documentId,
            @RequestBody ShareRequest request) {
        try {
            Document document = documentService.shareDocumentWithUser(
                    documentId, request.getOwnerUserId(), request.getShareWithUserId());
            return ResponseEntity.ok(document);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PatchMapping("/{documentId}/unshare")
    public ResponseEntity<Document> unshareDocument(
            @PathVariable Long documentId,
            @RequestBody UnshareDocumentRequest request
    ) {
        Document document = documentService.unshareDocument(
                documentId,
                request.getUserIds()
        );
        return ResponseEntity.ok(document);
    }



    @PostMapping("/{documentId}/share-signed")
    public ResponseEntity<?> shareSignedDocument(
            @PathVariable Long documentId,
            @RequestBody ShareSignedRequest request) {
        try {
            // Validate request
            if (request.getOwnerUserId() == null) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Owner user ID is required"));
            }

            if (request.getShareWithUserId() == null) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Share with user ID is required"));
            }

            // Check if document exists and is signed
            Document document = documentService.getDocument(documentId);
            if (document.getStatus() != DocumentStatus.SIGNED &&
                    document.getStatus() != DocumentStatus.SIGNED_AND_SHARED) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Only signed documents can be shared using this endpoint"));
            }

            // Share the signed document - now returns DocumentForward
            DocumentForward forward = documentService.shareSignedDocument(
                    documentId,
                    request.getOwnerUserId(),
                    request.getShareWithUserId(),
                    request.getShareMessage());

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Signed document shared successfully");
            response.put("data", forward);
            response.put("document", forward.getDocument()); // Include document info if needed
            response.put("sharedWithUserId", request.getShareWithUserId());
            response.put("permissions", Map.of(
                    "allowDownload", request.isAllowDownload(),
                    "allowForwarding", request.isAllowForwarding(),
                    "expiresAt", request.getExpiresAt(),
                    "isSignedDocument", forward.isSignedDocument()
            ));

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Error sharing signed document: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error sharing signed document: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to share signed document"));
        }
    }


    @PostMapping("/forwards/{forwardId}/accept/{userId}")
    public ResponseEntity<DocumentForward> acceptForward(
            @PathVariable Long forwardId,
            @PathVariable Long userId) {
        try {
            DocumentForward forward = documentService.acceptForward(forwardId, userId);
            return ResponseEntity.ok(forward);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/forwards/{forwardId}/reject")
    public ResponseEntity<?> rejectForward(
            @PathVariable Long forwardId,
            @RequestParam("user_id") Long userId) {
        try {
            DocumentForward forward = documentService.rejectForward(forwardId, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Forward rejected successfully");
            response.put("data", forward);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error rejecting forward: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/forwards/{forwardId}/revoke")
    public ResponseEntity<?> revokeForward(
            @PathVariable Long forwardId,
            @RequestParam("user_id") Long userId) {
        try {
            DocumentForward forward = documentService.revokeForward(forwardId, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Forward revoked successfully");
            response.put("data", forward);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error revoking forward: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}/accessible")
    public ResponseEntity<List<Document>> getAccessibleDocuments(@PathVariable Long userId) {
        List<Document> documents = documentService.getAccessibleDocuments(userId);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/user/{userId}/forwards/received")
    public ResponseEntity<List<DocumentForward>> getReceivedForwards(@PathVariable Long userId) {
        List<DocumentForward> forwards = documentService.getReceivedForwards(userId);
        return ResponseEntity.ok(forwards);
    }

    /**
     * Endpoint to download a file by its path
     * Example URL: /api/documents/download/1/documents/abc123.pdf
     */
    @GetMapping("/download/{userId}/{subFolder}/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String userId,
            @PathVariable String subFolder,
            @PathVariable String fileName,
            HttpServletRequest request) {

        String filePath = userId + "/" + subFolder + "/" + fileName;

        Resource resource = fileStorageService.loadFileAsResource(filePath);

        // Determine file's content type
        String contentType;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException e) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    // Get forwards with status filter
    @GetMapping("/forwards")
    public ResponseEntity<?> getForwards(
            @RequestParam("user_id") Long userId,
            @RequestParam(value = "status", required = false) ForwardStatus status,
            @RequestParam(value = "signed_only", defaultValue = "false") boolean signedOnly) {

        try {
            List<DocumentForward> forwards;

            if (status != null) {
                if (signedOnly) {
                    forwards = documentService.getSignedDocumentForwardsByStatus(userId, status);
                } else {
                    forwards = documentForwardRepository.findByForwardedToIdAndStatus(userId, status);
                }
            } else {
                if (signedOnly) {
                    forwards = documentService.getSignedDocumentForwardsForUser(userId);
                } else {
                    forwards = documentService.getReceivedForwards(userId);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", forwards);
            response.put("count", forwards.size());
            response.put("signedOnly", signedOnly);

            if (status != null) {
                response.put("status", status);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching forwards: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to fetch forwards"));
        }
    }

    // Get forward statistics
    @GetMapping("/forwards/statistics")
    public ResponseEntity<?> getForwardStatistics(@RequestParam("user_id") Long userId) {
        try {
            long totalForwards = documentService.countAllForwardsForUser(userId);
            long pendingForwards = documentService.countPendingForwards(userId);
            long acceptedForwards = documentForwardRepository.countByForwardedToIdAndStatus(
                    userId, ForwardStatus.ACCEPTED);
            long signedDocumentForwards = documentForwardRepository
                    .findByForwardedToId(userId)
                    .stream()
                    .filter(DocumentForward::isSignedDocument)
                    .count();

            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalForwards", totalForwards);
            statistics.put("pendingForwards", pendingForwards);
            statistics.put("acceptedForwards", acceptedForwards);
            statistics.put("signedDocumentForwards", signedDocumentForwards);
            statistics.put("rejectedForwards", totalForwards - pendingForwards - acceptedForwards);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("statistics", statistics);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching forward statistics: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to fetch forward statistics"));
        }
    }

    @GetMapping("/view/{userId}/{subFolder}/{fileName:.+}")
    public ResponseEntity<Resource> viewFile(
            @PathVariable String userId,
            @PathVariable String subFolder,
            @PathVariable String fileName,
            HttpServletRequest request) {

        String filePath = userId + "/" + subFolder + "/" + fileName;

        Resource resource = fileStorageService.loadFileAsResource(filePath);

        // Determine file's content type
        String contentType;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException e) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    /**
     * Optional endpoint to get the direct download link for a file
     */
    @GetMapping("/link/{userId}/{subFolder}/{fileName:.+}")
    public String getFileLink(
            @PathVariable String userId,
            @PathVariable String subFolder,
            @PathVariable String fileName) {

        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/documents/view/")
                .path(userId + "/" + subFolder + "/" + fileName)
                .toUriString();
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<?> deleteDocument(
            @PathVariable Long documentId,
            @RequestParam("user_id") Long userId
    ){
        documentService.deleteDocument(documentId, userId);
        return ResponseEntity.noContent().build();
    }





    private Map<String, Object> createPaginationInfo(Page<PdfUploadResponse> page, int currentPage, int limit, int offset, long totalItems) {
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("currentPage", currentPage);
        pagination.put("itemsPerPage", limit);
        pagination.put("offset", offset);
        pagination.put("totalItems", totalItems);
        pagination.put("totalPages", page.getTotalPages());
        pagination.put("hasNext", page.hasNext());
        pagination.put("hasPrevious", page.hasPrevious());
        return pagination;
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
}
