package com.github.ws_ncip_pnpki.dto;

import lombok.Data;

import java.util.List;

public class UnshareDocumentRequest {
    private List<Long> userIds;

    public List<Long> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<Long> userIds) {
        this.userIds = userIds;
    }
}
