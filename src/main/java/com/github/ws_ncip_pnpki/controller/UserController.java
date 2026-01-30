package com.github.ws_ncip_pnpki.controller;

import com.github.ws_ncip_pnpki.dto.PdfUploadResponse;
import com.github.ws_ncip_pnpki.dto.UserListResponse;
import com.github.ws_ncip_pnpki.dto.UserResponse;
import com.github.ws_ncip_pnpki.dto.UserSearchResponse;
import com.github.ws_ncip_pnpki.model.User;
import com.github.ws_ncip_pnpki.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
@Slf4j
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<?> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(value = "search", required = false) String search
    ){
        try {
            Page<UserResponse> usersPage = userService.getAllUsers(page, limit, offset, sortBy, sortDirection, search);
            long totalCount = userService.getTotalCount();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", usersPage.getContent());
            response.put("total", createPaginationInfo(usersPage, page, limit, offset, totalCount));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching all certificates", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to fetch certificates"));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "excludeCurrent", defaultValue = "true") boolean excludeCurrent,
            @RequestParam(value = "currentUserId", required = false) Long currentUserId,
            @RequestParam(value = "documentId") Long documentId) {

        try {
            List<User> users = userService.searchUsers(query, excludeCurrent, currentUserId, documentId);

            // get id, username, email from users list
            List<UserSearchResponse> lookupUsers = users.stream()
                    .map(user -> new UserSearchResponse(
                            user.getId(), user.getUsername(), user.getEmail(), user.getRoles()
                    )).toList();


            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", lookupUsers);
            response.put("count", users.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error searching users: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to search users"));
        }
    }

    private Map<String, Object> createPaginationInfo(Page<UserResponse> page, int currentPage, int limit, int offset, long totalItems) {
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
