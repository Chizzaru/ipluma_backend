package com.github.ws_ncip_pnpki.service;

import com.github.ws_ncip_pnpki.dto.*;
import com.github.ws_ncip_pnpki.model.*;
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
    public void unshare(Long ownerId, Long documentId, List<Long> userIds){
        userIds.forEach(id-> {
            try {
                documentSharedRepository.deleteByDocumentIdAndUserId(documentId, id);
            }catch (Exception ex){
                throw new RuntimeException(ex);
            }
        });

        List<DocumentShared> result =
                documentSharedRepository.findByIdDocumentId(documentId);

        if (result.isEmpty()) {
            Document doc = documentRepository.findById(documentId).orElseThrow();
            doc.setAvailableForSigning(true);
            doc.setAvailableForViewing(true);
            doc.setStatus(DocumentStatus.UPLOADED);
        }
    }

    @Transactional
    public ShareV2Response shareDoc(Long documentId, boolean downloadable, String message, List<ShareV2Request.User> users){
        Document doc = documentRepository.findById(documentId).orElseThrow();
        String sharedMessage = message.isBlank() ? "Sharing PDF document" : message;
        users.forEach(user -> {
            User u = userRepository.findById(user.getUserId()).orElseThrow();
            String permission = user.getPermission();
            int step = user.getStep();
            boolean parallel = user.isParallel();

            DocumentShared ds = new DocumentShared(doc, u, downloadable, permission, step, parallel);
            documentSharedRepository.save(ds);

            //Create notification
            Notification notification = new Notification();
            notification.setToUser(ds.getUser());
            notification.setFromUser(doc.getOwner());
            notification.setTitle("Document Share");
            notification.setMessage(sharedMessage);

            notificationRepository.save(notification);

        });

        if(users.stream().map(ShareV2Request.User::getPermission).anyMatch("sign"::equals)){
            doc.setAvailableForViewing(true);
            doc.setAvailableForSigning(true);
        }else{
            doc.setAvailableForViewing(true);
            doc.setAvailableForSigning(false);
        }

        doc.markAsShared();

        ShareV2Response response = new ShareV2Response();
        response.setId(doc.getId());
        response.setFileName(doc.getFileName());
        response.setFilePath(doc.getFilePath());
        response.setFileType(doc.getFileType());
        response.setFileSize(doc.getFileSize());
        response.setStatus(doc.getStatus());
        response.setUploadedAt(doc.getUploadedAt());

        response.setOwnerDetails(new ShareResponse.User(doc.getOwner().getId(), doc.getOwner().getUsername(), doc.getOwner().getEmail()));

        List<DocumentShared> ds = documentSharedRepository.findByIdDocumentId(doc.getId());
        List<ShareResponse.User> sharedToUsers =
                documentSharedRepository.findByIdDocumentId(doc.getId())
                        .stream()
                        .map(DocumentShared::getUser)
                        .map(user -> new ShareResponse.User(
                                user.getId(),
                                user.getUsername(),
                                user.getEmail()))
                        .toList();


        response.setSharedToUsers(sharedToUsers);
        response.setAvailableForViewing(doc.isAvailableForViewing());
        response.setAvailableForSigning(doc.isAvailableForSigning());

        List<ShareResponse.SignerStep> signerSteps = ds.stream()
                .map(dShared-> {
                    ShareResponse.User su = new ShareResponse.User(
                            dShared.getUser().getId(),
                            dShared.getUser().getUsername(),
                            dShared.getUser().getEmail()
                    );

                    ShareResponse.SignerStep step = new ShareResponse.SignerStep();
                    step.setStep(dShared.getStepNumber());
                    step.setUserId(dShared.getUser().getId());
                    step.setUser(su);
                    step.setPermission(dShared.getPermission());
                    step.setHasSigned(dShared.getSignedAt() != null);
                    step.setParallel(dShared.isParallel());
                    return step;
                }).toList();
        response.setSignerSteps(signerSteps);


        return response;

    }

    @Transactional
    public void share(Long documentId, Long userId, String message, boolean isDownloadable, String permission, int step, boolean isParallel){
        Document doc = documentRepository.findById(documentId).orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();
        String sharedMessage = message.isBlank() ? "Sharing PDF document" : message;
        DocumentShared ds = new DocumentShared(doc, user, isDownloadable, permission, step, isParallel);
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
        notification.setToUser(ds.getUser());
        notification.setFromUser(doc.getOwner());
        notification.setTitle("Document Share");
        notification.setMessage(sharedMessage);

        notificationRepository.save(notification);
    }

    public Page<ShareResponse> getSharedDocuments(Long userId, int page, int limit, int offset, String sortBy, String sortDirection, String search){
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



        return documentPage.map(document -> convertToSharedResponse(document, userId));
    }

    private ShareResponse convertToSharedResponse(Document document, Long userId) {

        List<DocumentShared> ds = documentSharedRepository.findByIdDocumentId(document.getId());

        DocumentShared shared =
                documentSharedRepository
                        .findByIdUserIdAndIdDocumentId(userId, document.getId())
                        .orElse(null);

        List<ShareResponse.User> sharedToUsers =
                documentSharedRepository.findByIdDocumentId(document.getId())
                        .stream()
                        .map(DocumentShared::getUser)
                        .map(user -> new ShareResponse.User(
                                user.getId(),
                                user.getUsername(),
                                user.getEmail()))
                        .toList();

        return getShareResponse(document, sharedToUsers, shared, ds);

    }

    private static ShareResponse getShareResponse(Document document, List<ShareResponse.User> sharedToUsers, DocumentShared shared, List<DocumentShared> ds) {
        ShareResponse response = new ShareResponse();
        response.setId(document.getId());
        response.setFileName(document.getFileName());
        response.setFilePath(document.getFilePath());
        response.setFileType(document.getFileType());
        response.setFileSize(document.getFileSize());
        response.setStatus(document.getStatus());
        response.setUploadedAt(document.getUploadedAt());
        response.setOwnerDetails(new ShareResponse.User(document.getOwner().getId(), document.getOwner().getUsername(), document.getOwner().getEmail()));


        response.setSharedToUsers(sharedToUsers);
        response.setAvailableForViewing(document.isAvailableForViewing());
        response.setAvailableForSigning(document.isAvailableForSigning());

        List<ShareResponse.SignerStep> signerSteps = ds.stream()
                .map(dShared-> {
                    ShareResponse.User su = new ShareResponse.User(
                            dShared.getUser().getId(),
                            dShared.getUser().getUsername(),
                            dShared.getUser().getEmail()
                    );

                    ShareResponse.SignerStep step = new ShareResponse.SignerStep();
                    step.setStep(dShared.getStepNumber());
                    step.setUserId(dShared.getUser().getId());
                    step.setUser(su);
                    step.setPermission(dShared.getPermission());
                    step.setHasSigned(dShared.getSignedAt() != null);
                    step.setParallel(dShared.isParallel());
                    return step;
                }).toList();
        response.setSignerSteps(signerSteps);
        return response;
    }

    public DocumentShared findByUserIdAndDocumentId(Long userId, Long documentId){
        return documentSharedRepository.findByIdUserIdAndIdDocumentId(userId, documentId).orElseThrow();
    }

    public void saveUpdate(DocumentShared ds){
        documentSharedRepository.save(ds);
    }

    private PdfUploadResponse convertToResponse(Document document, Long userId) {


        List<Long> nextUserIdsToSign = documentSharedRepository.findNextUserIdsToSign(document.getId());


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
                                user.getEmail(),"",
                                user.getRoles()))
                        .toList();

        return getPdfUploadResponse(document, sharedToUsers, shared, nextUserIdsToSign);

    }

    private static PdfUploadResponse getPdfUploadResponse(Document document, List<UserSearchResponse> sharedToUsers, DocumentShared shared, List<Long> nextUserIdsToSign) {
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
        response.setNextUserIdsToSign(nextUserIdsToSign);
        return response;
    }


}
