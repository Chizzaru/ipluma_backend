package com.github.ws_ncip_pnpki.controller;

import com.github.ws_ncip_pnpki.dto.SignatureResponseDTO;
import com.github.ws_ncip_pnpki.model.SignatureType;
import com.github.ws_ncip_pnpki.service.FileStorageService;
import com.github.ws_ncip_pnpki.service.SignatureService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/signatures")
@RequiredArgsConstructor
@Slf4j
public class SignatureController {

    private final SignatureService signatureService;
    private final FileStorageService fileStorageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SignatureResponseDTO> uploadSignature(
            @RequestParam("signature") MultipartFile file,
            @RequestParam("signatureType") SignatureType signatureType,
            @RequestParam("user_id") Long userId) {

        try {
            SignatureResponseDTO response = signatureService.uploadSignature(file, signatureType, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            log.error("IO error during signature upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/{signatureId}/set-default")
    public ResponseEntity<?> setDefaultSignature(
            @PathVariable Long signatureId,
            @RequestParam("user_id") Long userId) {

        try{
            signatureService.setDefaultSignatureV2(signatureId, userId);
            return ResponseEntity.ok().body(Map.of(
                    "message", "Certificate set as default successfully"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to set default certificate"
            ));
        }

    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SignatureResponseDTO> updateSignature(
            @PathVariable Long id,
            @RequestParam(value = "signature", required = false) MultipartFile file,
            @RequestParam(value = "signatureType", required = false) SignatureType signatureType,
            @RequestParam(value = "user_id", required = false) Long userId) {

        try {
            SignatureResponseDTO response = signatureService.updateSignature(id, file, signatureType, userId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid update request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            log.error("IO error during signature update", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    public ResponseEntity<List<SignatureResponseDTO>> getAllSignatures(
            @RequestParam(value = "type", required = false) SignatureType type,
            @RequestParam(value = "user_id") Long userId,
            @RequestParam(value = "user_roles") List<String> userRoles,
            @RequestParam(name = "only_defaults", required = false, defaultValue = "false") boolean onlyDefaults) {

        List<SignatureResponseDTO> signatures;

        if( onlyDefaults ) {
            return ResponseEntity.ok(signatureService.getAllDefaultSignatureByUser(userId));
        }

        /*boolean isAdmin = userRoles.contains("ROLE_ADMIN");
        boolean isUser = userRoles.contains("ROLE_USER");

        if (isAdmin && userId != null) {
            // Admin viewing by user
            signatures = (type != null)
                    ? signatureService.getAllSignatures(type)
                    : signatureService.getAllSignatures();
        } else if (isUser) {
            // Regular user (no admin privileges)
            signatures = (type != null)
                    ? signatureService.getAllSignaturesByUserId(userId, type)
                    : signatureService.getAllSignaturesByUserId(userId);
        } else {
            // Unknown or unauthorized role
            signatures = Collections.emptyList();
        }*/
        signatures = signatureService.getAllSignaturesByUserId(userId);
        return ResponseEntity.ok(signatures);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SignatureResponseDTO> getSignatureById(
            @PathVariable Long id) {
        SignatureResponseDTO signature = signatureService.getSignatureById(id);
        return ResponseEntity.ok(signature);
    }

    @GetMapping("/{id}/preview")
    public ResponseEntity<Resource> getSignaturePreview(
            @PathVariable Long id) {

        try {

            // Get signature metadata
            SignatureResponseDTO signature = signatureService.getSignatureById(id);

            // ✅ CRITICAL: Use signature.getFilePath() NOT signature.getFileName()
            // filePath contains: "userId/uuid.png"
            // fileName contains: "justin initial.png"
            Path filePath = fileStorageService.getFilePath(signature.getFilePath());

            log.info("Loading signature - ID: {}, FilePath: {}, FileName: {}",
                    id, signature.getFilePath(), signature.getFileName());
            log.debug("Absolute file path: {}", filePath.toAbsolutePath());

            // Check if file exists and is readable
            if (!Files.exists(filePath)) {
                log.error("File not found at path: {}", filePath.toAbsolutePath());
                log.error("Expected filePath from DB: {}", signature.getFilePath());
                return ResponseEntity.notFound().build();
            }

            if (!Files.isReadable(filePath)) {
                log.error("File not readable: {}", filePath.toAbsolutePath());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                // Determine content type from signature or file
                String contentType = signature.getContentType() != null
                        ? signature.getContentType()
                        : MediaType.IMAGE_PNG_VALUE;

                log.info("Successfully serving file: {} (stored as: {})",
                        signature.getFileName(), signature.getFilePath());

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "inline; filename=\"" + signature.getFileName() + "\"")
                        .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                        .header(HttpHeaders.PRAGMA, "no-cache")
                        .header(HttpHeaders.EXPIRES, "0")
                        .body(resource);
            } else {
                log.error("Resource exists but not readable: {}", filePath);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            log.error("Error retrieving signature preview for id: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> downloadSignatureFile(
            @PathVariable Long id,
            HttpServletRequest request) throws IOException {

        // Get signature to verify existence and get a file path
        SignatureResponseDTO signature = signatureService.getSignatureById(id);

        // Load file as Resource
        Resource resource = fileStorageService.loadFileAsResource(signature.getFilePath());

        // Try to determine a content type
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            log.info("Could not determine file type.");
        }

        // Fallback to the content type stored in a database
        if (contentType == null) {
            contentType = signature.getContentType();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSignature(
            @PathVariable Long id ) {  // ✅ FIXED: Use Authorization header

        try {
            signatureService.deleteSignature(id);
            return ResponseEntity.noContent().build();
        } catch (IOException e) {
            log.error("Error deleting signature: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}