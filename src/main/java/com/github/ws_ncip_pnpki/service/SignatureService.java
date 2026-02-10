package com.github.ws_ncip_pnpki.service;

import com.github.ws_ncip_pnpki.dto.SignatureResponseDTO;
import com.github.ws_ncip_pnpki.exception.ResourceNotFoundException;
import com.github.ws_ncip_pnpki.model.Signature;
import com.github.ws_ncip_pnpki.model.SignatureType;
import com.github.ws_ncip_pnpki.repository.SignatureRepository;
import com.github.ws_ncip_pnpki.repository.UserRepository;
import com.github.ws_ncip_pnpki.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;


import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SignatureService {

    private final SignatureRepository signatureRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final JwtUtil jwtUtil;


    @Transactional
    public SignatureResponseDTO uploadSignature(
            MultipartFile file,
            SignatureType signatureType,
            Long userId) throws IOException {

        validateFile(file);
        String filePath;

        if(signatureType == SignatureType.FULL){
            filePath = fileStorageService.storeFileV2(file, userId, "signatures");
        }else{
            filePath = fileStorageService.storeFileV2(file, userId, "signatures");
        }

        Signature signature = new Signature();
        signature.setFileName(file.getOriginalFilename());
        signature.setSignatureType(signatureType);
        signature.setFilePath(filePath);
        signature.setContentType(file.getContentType());
        signature.setFileSize(file.getSize());
        signature.setUserId(userId);

        Signature savedSignature = signatureRepository.save(signature);

        log.info("Signature uploaded successfully: {}", savedSignature.getId());
        return mapToResponseDTO(savedSignature);
    }

    @Transactional
    public void uploadSignatureSetDefault(
            MultipartFile file,
            SignatureType signatureType,
            Long userId) throws IOException {

        validateFile(file);
        String filePath;

        if(signatureType == SignatureType.FULL){
            filePath = fileStorageService.storeFileV2(file, userId, "signatures");
        }else{
            filePath = fileStorageService.storeFileV2(file, userId, "signatures");
        }

        Signature signature = new Signature();
        signature.setFileName(file.getOriginalFilename());
        signature.setSignatureType(signatureType);
        signature.setFilePath(filePath);
        signature.setContentType(file.getContentType());
        signature.setFileSize(file.getSize());
        signature.setDefault(true);
        signature.setUserId(userId);

        Signature savedSignature = signatureRepository.save(signature);

        log.info("Signature uploaded successfully: {}", savedSignature.getId());
        mapToResponseDTO(savedSignature);
    }

    @Transactional
    public SignatureResponseDTO updateSignature(
            Long id,
            MultipartFile file,
            SignatureType signatureType,
            Long userId) throws IOException {


        Signature signature = signatureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Signature not found"));

        if (file != null && !file.isEmpty()) {
            validateFile(file);

            // Delete old file
            try {
                fileStorageService.deleteFile(signature.getFilePath());
            } catch (IOException e) {
                log.warn("Failed to delete old file: {}", signature.getFilePath(), e);
            }

            // Store new file
            String newFilePath = fileStorageService.storeFile(file, userId, "signatures");
            signature.setFilePath(newFilePath);
            signature.setFileName(file.getOriginalFilename());
            signature.setContentType(file.getContentType());
            signature.setFileSize(file.getSize());
        }

        if (signatureType != null) {
            signature.setSignatureType(signatureType);
        }

        Signature updatedSignature = signatureRepository.save(signature);

        log.info("Signature updated successfully: {}", id);
        return mapToResponseDTO(updatedSignature);
    }

    @Transactional
    public SignatureResponseDTO setDefaultSignature(Long id, Long userId) {
        Signature signature = signatureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Signature not found"));
        signature.setDefault(true);
        signatureRepository.save(signature);
        return mapToResponseDTO(signature);
    }

    public Path getSignaturePath(Long id) {
        Optional<Signature> lookUp = signatureRepository.findById(id);
        if(lookUp.isEmpty()) {
            throw new ResourceNotFoundException("Signature not found");
        }
        return fileStorageService.getFilePath(lookUp.get().getFilePath());
    }

    public File getSignatureFile(Long id) {
        Optional<Signature> lookUp = signatureRepository.findById(id);
        if(lookUp.isEmpty()) {
            throw new ResourceNotFoundException("Signature not found");
        }
        return fileStorageService.getFile(lookUp.get().getFilePath());
    }

    public List<SignatureResponseDTO> getAllDefaultSignatureByUser(Long userId){
        return signatureRepository.findByUserIdAndIsDefault(userId, true)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    // ---------------- Admin-----------------------------
    public List<SignatureResponseDTO> getAllSignatures() {
        return signatureRepository.findAll()
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<SignatureResponseDTO> getAllSignatures(SignatureType type) {
        return signatureRepository.findBySignatureType(type)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }
    // ---------------- Admin-----------------------------

    // ---------------- User-----------------------------
    public List<SignatureResponseDTO> getAllSignaturesByUserId(Long userId) {
        return signatureRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<SignatureResponseDTO> getAllSignaturesByUserId(Long userId, SignatureType type) {
        return signatureRepository.findByUserIdAndSignatureType(userId, type)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }
    // ---------------- User-----------------------------

    public List<SignatureResponseDTO> getSignaturesByType(Long userId, SignatureType type) {

        return signatureRepository.findByUserIdAndSignatureType(userId, type)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public SignatureResponseDTO getSignatureById(Long id) {
        Signature signature = signatureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Signature not found"));

        System.out.println(signature);
        return mapToResponseDTO(signature);
    }

    public void setDefaultSignatureV2(Long signatureId, Long userId){

        Signature signature = signatureRepository.findById(signatureId)
                .orElseThrow(() -> new ResourceNotFoundException("Signature not found"));

        signatureRepository.findByUserIdAndSignatureTypeAndIsDefault(userId, signature.getSignatureType(), true)
                .ifPresent(previousDefault -> {
                    previousDefault.setDefault(false);
                    signatureRepository.save(previousDefault);
                });

        signature.setDefault(true);
        signatureRepository.save(signature);
    }

    @Transactional
    public void deleteSignature(Long id) throws IOException {
        Optional<Signature> lookUp = signatureRepository.findById(id);
        if(lookUp.isEmpty()) {
            throw new ResourceNotFoundException("Signature not found");
        }

        fileStorageService.deleteFile(lookUp.get().getFilePath());
        signatureRepository.delete(lookUp.get());

        log.info("Signature deleted successfully: {}", id);
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("image/png")) {
            throw new IllegalArgumentException("Only PNG files are allowed");
        }

        long maxSize = 5 * 1024 * 1024; // 5MB
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 5MB");
        }
    }

    private SignatureResponseDTO mapToResponseDTO(Signature signature) {
        /*String previewUrl = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/v1/signatures/")
                .path(String.valueOf(signature.getId()))
                .path("/preview")
                .toUriString();*/
        String previewUrl = String.format("v1/signatures/%d/preview", signature.getId());

        // ✅ CRITICAL: Include filePath in the DTO
        return new SignatureResponseDTO(
                signature.getId(),
                signature.getFileName(),      // Original name for display
                signature.getFilePath(),      // Actual stored path (userId/uuid.png)
                signature.getSignatureType(),
                signature.isDefault(),
                previewUrl,
                signature.getContentType(),
                signature.getCreatedAt(),
                signature.getFileSize()
        );
    }


}
