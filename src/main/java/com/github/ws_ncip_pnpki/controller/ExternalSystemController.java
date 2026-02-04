package com.github.ws_ncip_pnpki.controller;

import com.github.ws_ncip_pnpki.dto.AddExternalSystemRequest;
import com.github.ws_ncip_pnpki.dto.AddExternalSystemResponse;
import com.github.ws_ncip_pnpki.model.ExternalSystem;
import com.github.ws_ncip_pnpki.service.ExternalSystemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Slf4j
public class ExternalSystemController {

    private final ExternalSystemService externalSystemService;

    @PostMapping("/external-systems")
    public ResponseEntity<AddExternalSystemResponse> addExternalSystem(
            @RequestBody AddExternalSystemRequest request
            ){
        AddExternalSystemResponse response = convertToResponse(
                externalSystemService.saveWithCustomKeyLength(request.applicationName, request.applicationUrl, 32)
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @GetMapping("/external-systems")
    public ResponseEntity<?> getSharedDocument(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(required = false) String search
    ){
        try{
            Page<AddExternalSystemResponse> externalSystemPage;
            long totalCount = 0;

            externalSystemPage = externalSystemService.getAll(page, limit, offset, sortBy, sortDirection, search);
            totalCount = externalSystemPage.getTotalElements();


            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", externalSystemPage.getContent());
            response.put("pagination", createPaginationInfo(externalSystemPage, page, limit, offset, totalCount));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching all shared documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse());
        }
    }


    private AddExternalSystemResponse convertToResponse(ExternalSystem es){
        AddExternalSystemResponse response =  new AddExternalSystemResponse();
        response.setId(es.getId());
        response.setApplicationName(es.getAppName());
        response.setApplicationUrl(es.getAppUrl());
        response.setSecretKey(es.getSecretKey());
        response.setCreatedAt(es.getCreatedAt());
        return response;
    }

    private Map<String, Object> createPaginationInfo(Page<AddExternalSystemResponse> page, int currentPage, int limit, int offset, long totalItems) {
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

    private Map<String, Object> createErrorResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "Failed to fetch external system records");
        return response;
    }
}
