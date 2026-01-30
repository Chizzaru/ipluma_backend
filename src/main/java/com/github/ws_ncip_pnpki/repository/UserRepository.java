package com.github.ws_ncip_pnpki.repository;

import com.github.ws_ncip_pnpki.model.Document;
import com.github.ws_ncip_pnpki.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);

    // ✅ Custom query to return only the user ID
    @Query("SELECT u.id FROM User u WHERE u.username = :username")
    Optional<Long> findIdByUsername(@Param("username") String username);

    Page<User> findByEmailOrUsernameContaining(String email, String username, Pageable pageable);

    @Query("""
       SELECT u FROM User u
       WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
          OR LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
       """)
    List<User> searchUsers(@Param("searchTerm") String searchTerm);

    @Query("""
       SELECT u FROM User u
       WHERE u.id <> :excludeUserId
         AND (LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
           OR LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
       """)
    List<User> searchUsersExcludingCurrent(@Param("searchTerm") String searchTerm,
                                           @Param("excludeUserId") Long excludeUserId);

    // Get only users who do NOT yet have a documentSharedList entry with the given documentId.
    @Query("""
        SELECT u
        FROM User u
        WHERE u.id <> :excludeId
          AND NOT EXISTS (
              SELECT 1
              FROM DocumentShared d
              WHERE d.user = u
                AND d.document.id = :documentId
          )
    """)
    List<User> findAllByIdNot(
            @Param("excludeId") Long excludeId,
            @Param("documentId") Long documentId);

    // Find all users except multiple IDs
    @Query("SELECT u FROM User u WHERE u.id NOT IN :excludeIds")
    List<User> findAllByIdNotIn(@Param("excludeIds") List<Long> excludeIds);

    // Find users by role
    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<User> findByRoleName(@Param("roleName") String roleName);


    // Find by username or email (for login/authentication)
    @Query("SELECT u FROM User u WHERE u.username = :usernameOrEmail OR u.email = :usernameOrEmail")
    Optional<User> findByUsernameOrEmail(@Param("usernameOrEmail") String usernameOrEmail);


}
