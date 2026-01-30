package com.github.ws_ncip_pnpki.controller;

import com.github.ws_ncip_pnpki.dto.PdfUploadResponse;
import com.github.ws_ncip_pnpki.dto.ShareDocRequest;
import com.github.ws_ncip_pnpki.service.DocumentSharedService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v2")
@Slf4j
public class DocumentSharedController {

    private final DocumentSharedService documentSharedService;

    @Autowired
    public DocumentSharedController(DocumentSharedService documentSharedService) {
        this.documentSharedService = documentSharedService;
    }

    @PostMapping("/documents/share")
    public ResponseEntity<?> share(@RequestBody ShareDocRequest shareDocRequest) {

        List<Long> userIds = shareDocRequest.getUser_ids();
        List<ShareDocRequest.Steps> steps = shareDocRequest.getSteps();

        for (Long userId : userIds) {

            int step = 0;

            // steps are optional
            if (steps != null && !steps.isEmpty()) {
                step = steps.stream()
                        .filter(s -> s.getUser() != null)
                        .filter(s -> userId.equals(s.getUser().getId()))
                        .map(ShareDocRequest.Steps::getStep)
                        .findFirst()
                        .orElse(0); // user not in steps → no step
            }


            documentSharedService.share(
                    shareDocRequest.getDocument_id(),
                    userId,
                    shareDocRequest.getMessage(),
                    shareDocRequest.getDownloadable(),
                    shareDocRequest.getPermission(),
                    step   // can be null
            );
        }

        return ResponseEntity.ok().build();
    }


    @GetMapping("/documents")
    public ResponseEntity<?> getSharedDocument(
            @RequestParam("user_id") Long userId,
            @RequestParam("user_roles") List<String> userRoles,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(required = false) String search
    ){
        try{
            Page<PdfUploadResponse> documentPage;
            long totalCount;

            // get shared and shared to me
            documentPage = documentSharedService.getSharedDocuments(userId, page, limit, offset, sortBy, sortDirection, search);
            totalCount = documentPage.getTotalElements();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", documentPage.getContent());
            response.put("pagination", createPaginationInfo(documentPage, page, limit, offset, totalCount));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching all shared documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to fetch shared documents"));
        }
    }



    private Map<String, Object> createPaginationInfo(Page<PdfUploadResponse> page, int currentPage, int limit, int offset, long totalItems) {
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


}
