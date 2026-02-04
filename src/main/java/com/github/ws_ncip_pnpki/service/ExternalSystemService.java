package com.github.ws_ncip_pnpki.service;

import com.github.ws_ncip_pnpki.dto.AddExternalSystemResponse;
import com.github.ws_ncip_pnpki.dto.OffsetBasedPageRequest;
import com.github.ws_ncip_pnpki.model.Document;
import com.github.ws_ncip_pnpki.model.ExternalSystem;
import com.github.ws_ncip_pnpki.repository.ExternalSystemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExternalSystemService {

    private final ExternalSystemRepository externalSystemRepository;

    // SecureRandom for generating cryptographically secure keys
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // You can configure key length via properties if needed
    private static final int DEFAULT_KEY_LENGTH = 32; // 256 bits

    /**
     * Generates a secure random secret key
     * @return Base64 encoded secret key
     */
    private String generateSecretKey() {
        // Generate random bytes
        byte[] keyBytes = new byte[DEFAULT_KEY_LENGTH];
        SECURE_RANDOM.nextBytes(keyBytes);

        // Encode to Base64 for storage
        return Base64.getEncoder().encodeToString(keyBytes);
    }

    /**
     * Alternative method with configurable key length
     * @param keyLengthInBytes length of key in bytes
     * @return Base64 encoded secret key
     */
    private String generateSecretKey(int keyLengthInBytes) {
        if (keyLengthInBytes < 16) {
            throw new IllegalArgumentException("Key length should be at least 16 bytes (128 bits) for security");
        }

        byte[] keyBytes = new byte[keyLengthInBytes];
        SECURE_RANDOM.nextBytes(keyBytes);
        return Base64.getEncoder().encodeToString(keyBytes);
    }


    @Transactional
    public ExternalSystem save(String appName, String appUrl){
        try{
            ExternalSystem es = new ExternalSystem();
            es.setAppName(appName);
            es.setAppUrl(appUrl);

            // Generate and set secret key
            String secretKey = generateSecretKey();
            es.setSecretKey(secretKey);

            es.setSecretKey(secretKey);
            return externalSystemRepository.save(es);
        }catch (Exception ex){
            throw new RuntimeException("Error saving new External System record");
        }
    }

    /**
     * Alternative save method that allows specifying key length
     */
    @Transactional
    public ExternalSystem saveWithCustomKeyLength(String appName, String appUrl, int keyLengthInBytes) {
        try {
            ExternalSystem es = new ExternalSystem();
            es.setAppName(appName);
            es.setAppUrl(appUrl);

            // Generate and set secret key with custom length
            String secretKey = generateSecretKey(keyLengthInBytes);
            es.setSecretKey(secretKey);

            return externalSystemRepository.save(es);
        } catch (Exception ex) {
            throw new RuntimeException("Error saving new External System record", ex);
        }
    }



    @Cacheable(value = "corsOrigins", unless = "#results.isEmpty()")
    public List<String> getAllActiveExternalSystemUrls(){
        return externalSystemRepository.findAll()
                .stream()
                .map(ExternalSystem::getAppUrl)
                .filter(url -> url != null && !url.trim().isEmpty())
                .distinct()
                .toList();
    }

    public List<ExternalSystem> getAll(){
        return externalSystemRepository.findAll();
    }

    public Page<AddExternalSystemResponse> getAll(int page, int limit, int offset, String sortBy, String sortDirection, String search) {

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
        Page<ExternalSystem> externalSystemPage = externalSystemRepository.findAll(pageable);

        return externalSystemPage.map(this::convertToResponse);

    }

    private AddExternalSystemResponse convertToResponse(ExternalSystem es){
        AddExternalSystemResponse response = new AddExternalSystemResponse();
        response.setId(es.getId());
        response.setApplicationName(es.getAppName());
        response.setApplicationUrl(es.getAppUrl());
        response.setCreatedAt(es.getCreatedAt());
        response.setSecretKey(es.getSecretKey());
        return response;
    }
}
