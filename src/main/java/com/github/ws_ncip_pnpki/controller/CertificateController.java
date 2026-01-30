package com.github.ws_ncip_pnpki.controller;


import com.github.ws_ncip_pnpki.dto.DefaultCertResponse;
import com.github.ws_ncip_pnpki.model.Certificate;
import com.github.ws_ncip_pnpki.service.CertificateService;
import com.github.ws_ncip_pnpki.service.FileStorageService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/certificates")
@RequiredArgsConstructor
@Slf4j
public class CertificateController {

    private final CertificateService certificateService;

    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadCertificate(
            @RequestParam("user_id") Long userId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("password") String password) {

        try {

            Certificate certificate = certificateService.uploadCertificate(userId, file, password);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Certificate uploaded successfully");
            response.put("data", certificate);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));

        } catch (Exception e) {
            log.error("Error uploading certificate", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to upload certificate: " + e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserCertificates(@PathVariable Long userId) {
        try {
            List<Certificate> certificates = certificateService.getUserCertificates(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", certificates);
            response.put("count", certificates.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching certificates", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to fetch certificates"));
        }
    }

    @GetMapping("/{certificateId}")
    public ResponseEntity<?> getCertificate(
            @PathVariable Long certificateId,
            @RequestParam Long userId) {

        try {
            Certificate certificate = certificateService.getCertificate(certificateId, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", certificate);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(e.getMessage()));

        } catch (Exception e) {
            log.error("Error fetching certificate", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to fetch certificate"));
        }
    }

    @DeleteMapping("/{certificateId}")
    public ResponseEntity<?> deleteCertificate(
            @RequestParam("user_id") Long userId,
            @PathVariable Long certificateId) {

        try {

            certificateService.deleteCertificate(certificateId, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Certificate deleted successfully");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(e.getMessage()));

        } catch (Exception e) {
            log.error("Error deleting certificate", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to delete certificate"));
        }
    }


    @GetMapping("/default")
    public ResponseEntity<?> getDefaultCertificate(
            @RequestParam("user_id") Long userId
    ){
        Certificate certificate = certificateService.getDefaultCertificate(userId);
        DefaultCertResponse response = new DefaultCertResponse();
        response.setCertificateHash( certificate.getCertificateHash());
        response.setExpiresAt(certificate.getExpiresAt());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<?> getAllCertificates(
            @RequestParam("user_id") Long userId,
            @RequestParam("user_roles") List<String> userRoles,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {

        try {
            Page<Certificate> certificatesPage;
            long totalCount;
            // In a real application, you should verify if the user has admin privileges

            if(userRoles.contains("ROLE_ADMIN")) {
                certificatesPage = certificateService.getAllCertificates(page, limit, offset, sortBy, sortDirection);
                totalCount = certificateService.getTotalCertificateCount();
            }else{
                certificatesPage = certificateService.getUserCertificates(userId, page, limit, offset,sortBy, sortDirection);
                totalCount = certificateService.getUserCertificateCount(userId);
            }


            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", certificatesPage.getContent());
            response.put("pagination", createPaginationInfo(certificatesPage, page, limit, offset, totalCount));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching all certificates", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to fetch certificates"));
        }
    }

    @PatchMapping("/{certificateId}/set-default")
    public ResponseEntity<?> setDefaultCertificate(
            @PathVariable Long certificateId,
            @RequestParam("user_id") Long userId
    ){
        try {
            certificateService.setDefaultCertificate(certificateId, userId);
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

    @GetMapping("/user/{userId}/paginated")
    public ResponseEntity<?> getUserCertificatesPaginated(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset) {

        try {
            Page<Certificate> certificatesPage = certificateService.getUserCertificates(userId, page, limit, offset);
            long userCertificateCount = certificateService.getUserCertificateCount(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", certificatesPage.getContent());
            response.put("pagination", createPaginationInfo(certificatesPage, page, limit, offset, userCertificateCount));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching user certificates", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to fetch certificates"));
        }
    }

    private Map<String, Object> createPaginationInfo(Page<Certificate> page, int currentPage, int limit, int offset, long totalItems) {
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

}