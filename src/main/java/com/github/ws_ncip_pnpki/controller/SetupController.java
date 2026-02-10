package com.github.ws_ncip_pnpki.controller;

import com.github.ws_ncip_pnpki.dto.AuthResponse;
import com.github.ws_ncip_pnpki.dto.SetupRequest;
import com.github.ws_ncip_pnpki.model.User;
import com.github.ws_ncip_pnpki.service.CertificateService;
import com.github.ws_ncip_pnpki.service.SetupService;
import com.github.ws_ncip_pnpki.service.SignatureService;
import com.github.ws_ncip_pnpki.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
@Slf4j
public class SetupController {

    private final SetupService setupService;



    @PostMapping(value = "/setup")
    public ResponseEntity<?> setup(
            @Valid @ModelAttribute SetupRequest request
            ){
        AuthResponse response = setupService.saveSetup(request);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
