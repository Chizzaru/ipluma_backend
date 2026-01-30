package com.github.ws_ncip_pnpki.service;


import com.github.ws_ncip_pnpki.model.Certificate;
import com.github.ws_ncip_pnpki.model.User;
import com.github.ws_ncip_pnpki.repository.CertificateRepository;
import com.github.ws_ncip_pnpki.repository.UserRepository;
import com.github.ws_ncip_pnpki.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Key;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CertificateService {

    private final CertificateRepository certificateRepository;

    private final JwtUtil jwtUtil;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private UserRepository userRepository;

    @Value("${file.upload.dir}")
    private String uploadDir;


    @Transactional
    public Certificate uploadCertificate(Long userId, MultipartFile file, String password) throws Exception {

        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (!Objects.requireNonNull(file.getOriginalFilename()).toLowerCase().endsWith(".p12")
                && !file.getOriginalFilename().toLowerCase().endsWith(".pfx")) {
            throw new IllegalArgumentException("Only P12/PFX files are allowed");
        }

        // Calculate hash to check for duplicates
        byte[] fileBytes = file.getBytes();
        String certificateHash = calculateHash(fileBytes);

        // Check if a certificate already exists for this user
        if (certificateRepository.existsByUserIdAndCertificateHash(userId, certificateHash)) {
            throw new IllegalArgumentException("Certificate already exists for this user");
        }

        // Extract certificate details
        CertificateDetails details = extractCertificateDetails(fileBytes, password);

        // Create a directory if not exists
        Path uploadPath = Paths.get(uploadDir, String.valueOf(userId), "certificates");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate a unique file name
        String storedFileName = UUID.randomUUID() + ".p12";
        String filePath = fileStorageService.storeFile(file, userId, "certificates");

        // Create a certificate entity
        Certificate certificate = new Certificate();
        certificate.setUserId(userId);
        certificate.setFileName(file.getOriginalFilename());
        certificate.setStoredFileName(storedFileName);
        certificate.setCertificateHash(certificateHash);
        certificate.setFilePath(filePath);
        certificate.setFileSize(file.getSize());
        certificate.setIssuer(details.getIssuer());
        certificate.setSubject(details.getSubject());
        certificate.setExpiresAt(details.getExpiresAt());

        return certificateRepository.save(certificate);
    }

    public Path getCertPath(String certificateHash){
        Optional<Certificate> lookUp =  certificateRepository.findByCertificateHashIgnoreCase(certificateHash);
        return lookUp.map(Certificate::getFilePath).map(Paths::get).orElse(null);
    }

    public File getCertFile(String filePath){
        return fileStorageService.getFile(filePath);
    }

    public void setDefaultCertificate(Long certificateId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Certificate certificate = certificateRepository.findByIdAndUserId(certificateId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Certificate not found"));

        // Find and unset the previous default certificate
        certificateRepository.findByUserIdAndIsDefaultTrue(userId)
                .ifPresent(previousDefault -> {
                    previousDefault.setDefault(false);
                    certificateRepository.save(previousDefault);
                });

        // Set the new certificate as default
        certificate.setDefault(true);
        certificateRepository.save(certificate);
    }



    public Certificate getDefaultCertificate(Long userId){
        return certificateRepository.findByUserIdAndIsDefault(userId, true);
    }

    public List<Certificate> getUserCertificates(Long userId) {
        return certificateRepository.findByUserId(userId);
    }

    public Certificate getCertificate(Long certificateId, Long userId) {
        return certificateRepository.findByIdAndUserId(certificateId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Certificate not found"));
    }

    @Transactional
    public void deleteCertificate(Long certificateId, Long userId) throws IOException {

        Certificate certificate = certificateRepository.findByIdAndUserId(certificateId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Certificate not found"));

        // Delete a physical file
        Path filePath = Paths.get(certificate.getFilePath());
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }

        // Delete from database
        certificateRepository.deleteByIdAndUserId(certificateId, userId);
    }

    private String calculateHash(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);
        return HexFormat.of().formatHex(hash);
    }



    private CertificateDetails extractCertificateDetails(byte[] fileBytes, String password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(new java.io.ByteArrayInputStream(fileBytes), password.toCharArray());

        Enumeration<String> aliases = keyStore.aliases();
        if (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);

            if (cert != null) {
                String issuer = cert.getIssuerX500Principal().getName();
                String subject = cert.getSubjectX500Principal().getName();
                LocalDateTime expiresAt = Instant.ofEpochMilli(cert.getNotAfter().getTime())
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();

                return new CertificateDetails(issuer, subject, expiresAt);
            }
        }

        throw new IllegalArgumentException("Invalid certificate or password");
    }



    @lombok.Data
    @lombok.AllArgsConstructor
    private static class CertificateDetails {
        private String issuer;
        private String subject;
        private LocalDateTime expiresAt;
    }


    /**
     * Get all certificates with pagination (for admin/superuser)
     */
    public Page<Certificate> getAllCertificates(int page, int limit, int offset, String sortBy, String sortDirection) {
        // Convert from frontend's 1-based page to Spring's 0-based page
        int actualPage = page - 1;

        // Validate and adjust page number
        if (actualPage < 0) {
            actualPage = 0;
        }

        // Create sort
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);

        // Create pageable
        Pageable pageable = PageRequest.of(actualPage, limit, sort);

        // Execute query
        return certificateRepository.findAll(pageable);
    }

    public Page<Certificate> getUserCertificates(Long userId, int page, int limit, int offset, String sortBy, String sortDirection) {
        // Convert from frontend's 1-based page to Spring's 0-based page
        int actualPage = page - 1;

        // Validate and adjust page number
        if (actualPage < 0) {
            actualPage = 0;
        }

        // Create sort
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);

        // Create pageable
        Pageable pageable = PageRequest.of(actualPage, limit, sort);

        // Execute query
        return certificateRepository.findByUserId(userId, pageable);
    }


    /**
     * Get user certificates with pagination
     */
    public Page<Certificate> getUserCertificates(Long userId, int page, int limit, int offset) {
        int actualPage = (offset / limit) + page;
        Pageable pageable = PageRequest.of(actualPage, limit);
        return certificateRepository.findByUserId(userId, pageable);
    }

    /**
     * Get total count of all certificates
     */
    public long getTotalCertificateCount() {
        return certificateRepository.count();
    }

    /**
     * Get count of user's certificates
     */
    public long getUserCertificateCount(Long userId) {
        return certificateRepository.findByUserId(userId).size();
    }
}
