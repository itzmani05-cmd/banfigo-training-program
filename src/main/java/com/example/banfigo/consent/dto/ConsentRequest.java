package com.example.banfigo.consent.dto;

import com.example.banfigo.consent.entity.ConsentPermission;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Set;

public class ConsentRequest {

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotBlank(message = "Third-party provider name is required")
    @Size(max = 100, message = "Third-party provider name cannot exceed 100 characters")
    private String tppName;

    @NotEmpty(message = "At least one permission is required")
    private Set<ConsentPermission> permissions;

    // Must all belong to the customer (checked in ConsentService)
    @NotEmpty(message = "At least one account is required")
    private Set<Long> accountIds;

    @NotNull(message = "Expiry date is required")
    @Future(message = "Expiry date must be in the future")
    private LocalDateTime expiresAt;

    public ConsentRequest() {
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getTppName() {
        return tppName;
    }

    public void setTppName(String tppName) {
        this.tppName = tppName;
    }

    public Set<ConsentPermission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<ConsentPermission> permissions) {
        this.permissions = permissions;
    }

    public Set<Long> getAccountIds() {
        return accountIds;
    }

    public void setAccountIds(Set<Long> accountIds) {
        this.accountIds = accountIds;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
