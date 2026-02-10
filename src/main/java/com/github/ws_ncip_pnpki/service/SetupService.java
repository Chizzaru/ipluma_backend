package com.github.ws_ncip_pnpki.service;

import com.github.ws_ncip_pnpki.dto.AuthResponse;
import com.github.ws_ncip_pnpki.dto.SetupRequest;
import com.github.ws_ncip_pnpki.model.Role;
import com.github.ws_ncip_pnpki.model.SignatureType;
import com.github.ws_ncip_pnpki.model.User;
import com.github.ws_ncip_pnpki.repository.CertificateRepository;
import com.github.ws_ncip_pnpki.repository.SignatureRepository;
import com.github.ws_ncip_pnpki.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SetupService {

    private final UserRepository userRepository;
    private final CertificateRepository certificateRepository;
    private final CertificateService certificateService;
    private final SignatureRepository signatureRepository;
    private final SignatureService signatureService;
    private final UserService userService;


    @Transactional
    public AuthResponse saveSetup(SetupRequest request){
        User user = userRepository.findById(request.getUserId()).orElseThrow();

        // save certificate set default
        try {
            certificateService.uploadCertificateSetDefault( user.getId() ,request.getCertificate(), request.getCertPassword());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // save full signature
        try {
            signatureService.uploadSignatureSetDefault(request.getFullSignature(), SignatureType.FULL, user.getId());
        }catch (Exception e){
            throw new RuntimeException(e);
        }

        // save initial
        try {
            signatureService.uploadSignatureSetDefault(request.getInitialSignature(), SignatureType.INITIAL, user.getId());
        }catch (Exception e){
            throw new RuntimeException(e);
        }

        // update Password
        String newPassword = request.getNewPassword();
        if(newPassword != null){
            try{
                // Trim and check if not empty after trimming
                newPassword = newPassword.trim();
                if(!newPassword.isEmpty()){
                    userService.changePassword(user.getId(),newPassword);
                }
            }catch (Exception e){
                throw new RuntimeException("Failed to update password: " + e.getMessage(), e);
            }
        }

        user.setFinishedSetup(true);
        userRepository.save(user);

        return new AuthResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()), user.isFinishedSetup());
    }

}
