package com.github.ws_ncip_pnpki.service;

import com.github.ws_ncip_pnpki.dto.OffsetBasedPageRequest;
import com.github.ws_ncip_pnpki.dto.PdfUploadResponse;
import com.github.ws_ncip_pnpki.dto.ShareDocRequest;
import com.github.ws_ncip_pnpki.dto.UserSearchResponse;
import com.github.ws_ncip_pnpki.model.Document;
import com.github.ws_ncip_pnpki.model.DocumentShared;
import com.github.ws_ncip_pnpki.model.Notification;
import com.github.ws_ncip_pnpki.model.User;
import com.github.ws_ncip_pnpki.repository.DocumentRepository;
import com.github.ws_ncip_pnpki.repository.DocumentSharedRepository;
import com.github.ws_ncip_pnpki.repository.NotificationRepository;
import com.github.ws_ncip_pnpki.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class DocumentSharedService {

    private final DocumentSharedRepository documentSharedRepository;

    private final DocumentRepository documentRepository;

    private final UserRepository userRepository;

    private final NotificationRepository notificationRepository;

    @Autowired
    public DocumentSharedService(DocumentSharedRepository documentSharedRepository, DocumentRepository documentRepository, UserRepository userRepository, NotificationRepository notificationRepository) {
        this.documentSharedRepository = documentSharedRepository;
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void share(Long documentId, Long userId, String message, boolean isDownloadable, String permission, int step){
        Document doc = documentRepository.findById(documentId).orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();
        String sharedMessage = message.isBlank() ? "Sharing PDF document" : message;
        DocumentShared ds = new DocumentShared(doc, user, isDownloadable, permission, step);
        if(permission.equals("view")){
            doc.setAvailableForViewing(true);
            doc.setAvailableForSigning(false);
        }

        if(permission.equals("view_and_sign")){
            doc.setAvailableForViewing(true);
            doc.setAvailableForSigning(true);
        }


        documentSharedRepository.save(ds);
        documentRepository.save(doc);


        doc.markAsShared();

        //Create notification
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle("Document Share");
        notification.setMessage(sharedMessage);

        notificationRepository.save(notification);
    }

    public Page<PdfUploadResponse> getSharedDocuments(Long userId, int page, int limit, int offset, String sortBy, String sortDirection, String search){
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);

        Pageable pageable;

        if(offset > 0){
            pageable = new OffsetBasedPageRequest(offset, limit, sort);
        }else {
            // Fallback to regular page-based
            int actualPage = Math.max(0, page - 1);
            pageable = PageRequest.of(actualPage, limit, sort);
        }

        // Execute a query with search
        Page<Document> documentPage;
        if(search != null && !search.trim().isEmpty()){
            documentPage = documentRepository.searchSharedDocuments(userId, search, pageable);
        } else {
            documentPage = documentRepository.allSharedDocuments(userId, pageable);
        }

        return documentPage.map(document -> convertToResponse(document, userId));
    }


    private PdfUploadResponse convertToResponse(Document document, Long userId) {

        DocumentShared shared =
                documentSharedRepository
                        .findByIdUserIdAndIdDocumentId(userId, document.getId())
                        .orElse(null);

        List<UserSearchResponse> sharedToUsers =
                documentSharedRepository.findByIdDocumentId(document.getId())
                        .stream()
                        .map(DocumentShared::getUser)
                        .map(user -> new UserSearchResponse(
                                user.getId(),
                                user.getUsername(),
                                user.getEmail(),
                                user.getRoles()))
                        .toList();

        return getPdfUploadResponse(document, sharedToUsers, shared);

    }

    private static PdfUploadResponse getPdfUploadResponse(Document document, List<UserSearchResponse> sharedToUsers, DocumentShared shared) {
        PdfUploadResponse response = new PdfUploadResponse();
        response.setId(document.getId());
        response.setFileName(document.getFileName());
        response.setFilePath(document.getFilePath());
        response.setFileType(document.getFileType());
        response.setFileSize(document.getFileSize());
        response.setStatus(document.getStatus());
        response.setUploadedAt(document.getUploadedAt());

        response.setOwnerDetails(new PdfUploadResponse.OwnerDetails(
                document.getOwner().getId(),
                document.getOwner().getUsername(),
                document.getOwner().getEmail()
        ));

        response.setSharedToUsers(sharedToUsers);
        response.setAvailableForDownload(shared != null && shared.isDownloadable());
        response.setPermission(shared != null ? shared.getPermission() : null);
        response.setAvailableForViewing(document.isAvailableForViewing());
        response.setAvailableForSigning(document.isAvailableForSigning());
        return response;
    }


}
