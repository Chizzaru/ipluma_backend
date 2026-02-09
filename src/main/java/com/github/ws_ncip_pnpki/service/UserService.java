package com.github.ws_ncip_pnpki.service;

import com.github.ws_ncip_pnpki.dto.OffsetBasedPageRequest;
import com.github.ws_ncip_pnpki.dto.UserListResponse;
import com.github.ws_ncip_pnpki.dto.UserResponse;
import com.github.ws_ncip_pnpki.model.Document;
import com.github.ws_ncip_pnpki.model.Employee;
import com.github.ws_ncip_pnpki.model.User;
import com.github.ws_ncip_pnpki.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final TemporaryCredentialService temporaryCredentialService;

    private final EmployeeService employeeService;

    @Autowired
    public UserService(UserRepository userRepository, TemporaryCredentialService temporaryCredentialService, EmployeeService employeeService) {
        this.userRepository = userRepository;
        this.temporaryCredentialService = temporaryCredentialService;
        this.employeeService = employeeService;
    }


    public Page<UserResponse> getAllUsers(int page, int limit, int offset, String sortBy, String sortDirection, String search, Long userId) {

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);

        Pageable pageable;

        if(offset > 0){
            pageable = new OffsetBasedPageRequest(offset, limit, sort);
        }else{
            int actualPage = Math.max(0, page - 1);
            pageable = PageRequest.of(actualPage, limit, sort);
        }

        // Execute a query with search
        Page<User> userPage;
        if (search != null && !search.trim().isEmpty()) {
            userPage = userRepository.findByEmailOrUsernameContainingAndIdNot(search, search, userId, pageable);
        } else {
            userPage = userRepository.findAll(pageable);
        }

        return userPage.map(this::convertToUserResponse);
    }
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }


    public long getTotalCount() {
        return userRepository.count();
    }

    public List<User> searchUsers(String query, boolean excludeCurrent, Long currentUserId, Long documentId) {
        try {
            String searchTerm = "%" + (query != null ? query.trim() : "") + "%";

            if (query == null || query.trim().isEmpty()) {
                // If no query, return all users (excluding current if requested)
                if (excludeCurrent && currentUserId != null) {
                    return userRepository.findAllByIdNot(currentUserId, documentId);
                }
                return userRepository.findAll();
            } else {
                // Search with query
                if (excludeCurrent && currentUserId != null) {
                    return userRepository.searchUsersExcludingCurrent(searchTerm, currentUserId);
                }
                return userRepository.searchUsers(searchTerm);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to search users");
        }
    }

    public UserResponse getProfile(Long userId){
        User user = userRepository.findById(userId).orElseThrow();
        return convertToUserResponse(user);
    }

    @Transactional
    public void changePassword(Long userId, String newPassword){
        User user = userRepository.findById(userId).orElseThrow();
        user.setPassword(new BCryptPasswordEncoder().encode(newPassword));
        userRepository.save(user);
    }


    private UserResponse convertToUserResponse(User user) {

        Long employeeId = user.getEmployee().getId();

        String password = temporaryCredentialService.getTemporaryPassword(employeeId);

        boolean isTempEmailSent = temporaryCredentialService.tempEmailSent(employeeId);

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .password(password)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .role(user.getRoles())
                .tempEmailSent(isTempEmailSent)
                .build();
    }
}
