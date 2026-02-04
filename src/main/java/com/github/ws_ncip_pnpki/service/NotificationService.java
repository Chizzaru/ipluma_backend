package com.github.ws_ncip_pnpki.service;

import com.github.ws_ncip_pnpki.dto.OffsetBasedPageRequest;
import com.github.ws_ncip_pnpki.dto.PdfUploadResponse;
import com.github.ws_ncip_pnpki.dto.UserSearchResponse;
import com.github.ws_ncip_pnpki.model.Document;
import com.github.ws_ncip_pnpki.model.Notification;
import com.github.ws_ncip_pnpki.repository.NotificationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public NotificationService(NotificationRepository notificationRepository, SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public Page<Notification> allNotification(
            Long userId,
            int page,
            int limit,
            int offset,
            String sortBy,
            String sortDirection,
            String search){


        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);

        Pageable pageable;

        if(offset > 0){
            pageable = new OffsetBasedPageRequest(offset, limit, sort);
        }else{
            int actualPage = Math.max(0, page - 1);
            pageable = PageRequest.of(actualPage, limit, sort);
        }

        Page<Notification> notificationPage;
        if(search != null && !search.trim().isEmpty()){
            notificationPage = notificationRepository.searchNotification(userId, search, pageable);
        }else{
            notificationPage = notificationRepository.allNotification(userId, pageable);
        }

        return notificationPage;
    }

    public Long getUnreadNotifCount(Long toUserId){
        Long count = notificationRepository.countByToUser_IdAndOpenedFalse(toUserId);
        messagingTemplate.convertAndSend("/topic/notif-updates", count);
        return count;
    }


    public Notification getNotification(Long notificationId){
        return notificationRepository.findById(notificationId).orElseThrow();
    }

    @Transactional
    public void readNotification(Long notificationId){
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found"));
        notification.setOpened(true);
    }



    private PdfUploadResponse convertToResponse(Document document) {
        PdfUploadResponse response = new PdfUploadResponse();
        response.setId(document.getId());
        response.setFileName(document.getFileName());
        response.setFilePath(document.getFilePath());
        response.setFileType(document.getFileType());
        response.setFileSize(document.getFileSize());
        response.setStatus(document.getStatus());
        response.setUploadedAt(document.getUploadedAt());
        response.setOwnerDetails(new PdfUploadResponse.OwnerDetails(document.getOwner().getId(), document.getOwner().getUsername(), document.getOwner().getEmail()));
        response.setSharedToUsers(document.getSharedWithUsers().stream().map(user -> new UserSearchResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRoles())).toList());
        return response;
    }

}
