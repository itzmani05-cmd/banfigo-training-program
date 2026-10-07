package com.example.week1_backend_assesment.dto;

import jakarta.validation.constraints.Size;

// Optional body for reject / revoke: why the consent was turned down
public class ConsentDecisionRequest {

    @Size(max = 255, message = "Reason cannot exceed 255 characters")
    private String reason;

    public ConsentDecisionRequest() {
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
