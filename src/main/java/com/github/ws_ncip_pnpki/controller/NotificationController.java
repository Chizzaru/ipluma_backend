package com.github.ws_ncip_pnpki.controller;

import com.github.ws_ncip_pnpki.dto.NotificationResponse;
import com.github.ws_ncip_pnpki.model.Notification;
import com.github.ws_ncip_pnpki.model.User;
import com.github.ws_ncip_pnpki.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;
    @Autowired
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RequestMapping("/notifications")
    public ResponseEntity<?> getNotification(
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
            Page<Notification> notificationPage;
            long totalCount;

            // get shared and shared to me
            notificationPage = notificationService.allNotification(userId, page, limit, offset, sortBy, sortDirection, search);
            totalCount = notificationPage.getTotalElements();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", notificationPage.getContent().stream().map(this::convertResponse));
            response.put("pagination", createPaginationInfo(notificationPage, page, limit, offset, totalCount));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching all shared documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to fetch shared documents"));
        }
    }

    @GetMapping("/notifications/unread-count/{toUserId}")
    public Long getUnreadCount( @PathVariable("toUserId") Long toUserId){
        return notificationService.getUnreadNotifCount(toUserId);
    }

    @PatchMapping("/notifications/{notificationId}/read")
    public ResponseEntity<?> updateNotification(@PathVariable Long notificationId){
        notificationService.readNotification(notificationId);

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    private NotificationResponse convertResponse(Notification notification){
        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setFromUser(userToFromUser(notification.getFromUser()));
        response.setTitle(notification.getTitle());
        response.setMessage(notification.getMessage());
        response.setOpened(notification.isOpened());
        response.setCreatedAt(notification.getCreatedAt());
        return response;
    }

    private NotificationResponse.FromUser userToFromUser(User user){
        NotificationResponse.FromUser fromUser = new NotificationResponse.FromUser();
        fromUser.setId(user.getId());
        fromUser.setUsername(user.getUsername());
        return fromUser;
    }


    private Map<String, Object> createPaginationInfo(Page<Notification> page, int currentPage, int limit, int offset, long totalItems) {
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
