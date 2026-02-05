package com.github.ws_ncip_pnpki.controller;

import com.github.ws_ncip_pnpki.service.TemporaryCredentialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/temp")
@RequiredArgsConstructor
public class TemporaryCredentialController {

    private final TemporaryCredentialService temporaryCredentialService;


    @PostMapping("/create-all")
    public ResponseEntity<?> createAccounts(){

        temporaryCredentialService.savedTempCredential();

        return ResponseEntity.status(HttpStatus.CREATED).body("Success!");
    }
}
