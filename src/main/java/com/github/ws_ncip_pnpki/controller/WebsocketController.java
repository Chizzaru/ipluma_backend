package com.github.ws_ncip_pnpki.controller;

import com.github.ws_ncip_pnpki.dto.PdfUploadResponse;
import com.github.ws_ncip_pnpki.dto.WsDocUpdateRequest;
import com.github.ws_ncip_pnpki.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

/**
 * Backend Websocket Controller
 */
@Controller
public class WebsocketController {

    @Autowired
    private DocumentService documentService;

    @MessageMapping("/doc/locked-in")
    @SendTo("/topic/doc-updates")
    public PdfUploadResponse handleBlockedOthersForSigning(WsDocUpdateRequest request){
        return documentService.blockedOthersForSigning(request.getDocumentId(), request.getUserId());
    }

    @MessageMapping("/doc/locked-out")
    @SendTo("/topic/doc-updates")
    public PdfUploadResponse handleUnblockOthersForSigning(WsDocUpdateRequest request){
        return documentService.unblockOthersForSigning(request.getDocumentId(), request.getUserId());
    }

}
