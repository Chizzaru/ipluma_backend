package com.github.ws_ncip_pnpki.model;

public enum ForwardStatus {
    PENDING,    // Forward has been sent but not yet accepted/rejected
    ACCEPTED,   // Recipient has accepted the forward
    REJECTED,   // Recipient has rejected the forward
    EXPIRED,    // Forward has expired (if expiration was set)
    REVOKED     // Sender has revoked the forward
}
