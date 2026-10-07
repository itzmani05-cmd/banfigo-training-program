package com.example.banfigo.consent.dto;

import com.example.banfigo.consent.entity.ConsentPermission;
import com.example.banfigo.consent.entity.ConsentStatus;

import java.time.LocalDateTime;
import java.util.List;

public class ConsentResponse {
    private Long id;
    private Long customerId;
    private String customerName;
    private String tppName;
    private List<ConsentPermission> permissions;
    private List<String> accountNumbers;
    private ConsentStatus status;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime decidedAt;
    private String decidedBy;
    private String rejectionReason;

    public ConsentResponse() {
    }

    public ConsentResponse(Long id, Long customerId, String customerName, String tppName,
                           List<ConsentPermission> permissions, List<String> accountNumbers,
                           ConsentStatus status, LocalDateTime expiresAt, LocalDateTime createdAt,
                           String createdBy, LocalDateTime decidedAt, String decidedBy,
                           String rejectionReason) {
        this.id = id;
        this.customerId = customerId;
        this.customerName = customerName;
        this.tppName = tppName;
        this.permissions = permissions;
        this.accountNumbers = accountNumbers;
        this.status = status;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.decidedAt = decidedAt;
        this.decidedBy = decidedBy;
        this.rejectionReason = rejectionReason;
    }

    public Long getId() {
        return id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getTppName() {
        return tppName;
    }

    public List<ConsentPermission> getPermissions() {
        return permissions;
    }

    public List<String> getAccountNumbers() {
        return accountNumbers;
    }

    public ConsentStatus getStatus() {
        return status;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public String getDecidedBy() {
        return decidedBy;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }
}
