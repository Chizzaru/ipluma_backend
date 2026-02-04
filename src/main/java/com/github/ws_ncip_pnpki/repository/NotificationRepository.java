package com.github.ws_ncip_pnpki.repository;

import com.github.ws_ncip_pnpki.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {


    @Query("""
            SELECT t FROM Notification t
            INNER JOIN t.toUser u
            WHERE t.toUser.id = :userId
            """)
    Page<Notification> allNotification(
            @Param("userId") Long userId,
            Pageable pageable);


    @Query("""
            SELECT t FROM Notification t
            INNER JOIN t.toUser u
            WHERE t.toUser.id = :userId
            AND (LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(t.message) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Notification> searchNotification(
            @Param("userId") Long userId,
            @Param("search") String search,
            Pageable pageable);

    Long countByToUser_IdAndOpenedFalse(Long toUserId);
}
