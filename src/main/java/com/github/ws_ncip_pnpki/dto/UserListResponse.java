package com.github.ws_ncip_pnpki.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class UserListResponse {
    private List<UserResponse> users;
    private int currentPage;
    private int totalPages;
    private long totalItems;
    private boolean hasNext;
    private boolean hasPrevious;
    private int pageSize;
}