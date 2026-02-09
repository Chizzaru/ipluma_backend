package com.github.ws_ncip_pnpki.controller;


import com.github.ws_ncip_pnpki.dto.SendEmailDefaultPasswordRequest;
import com.github.ws_ncip_pnpki.service.EmailNotificationService;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;

@RestController
@RequestMapping("/api/v1/email")
public class EmailController {

    @Autowired
    private EmailNotificationService emailService;


    @PostMapping("/send")
    public ResponseEntity<?> sendDefaultPasswordEmail(
            @RequestBody SendEmailDefaultPasswordRequest request
            ) throws MessagingException, UnsupportedEncodingException {

        emailService.sendHtmlEmail(request.getUserId());
        return ResponseEntity.status(HttpStatus.OK).build();

    }

}
