package com.example.week1_backend_assesment.controller;

import com.example.week1_backend_assesment.dto.ConsentDecisionRequest;
import com.example.week1_backend_assesment.dto.ConsentRequest;
import com.example.week1_backend_assesment.dto.ConsentResponse;
import com.example.week1_backend_assesment.entity.ConsentStatus;
import com.example.week1_backend_assesment.service.ConsentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/consents")
public class ConsentController {

    private final ConsentService consentService;

    public ConsentController(ConsentService consentService) {
        this.consentService = consentService;
    }

    @PostMapping
    public ResponseEntity<ConsentResponse> createConsent(@Valid @RequestBody ConsentRequest request,
                                                         @AuthenticationPrincipal Jwt jwt) {
        return new ResponseEntity<>(consentService.createConsent(request, username(jwt)), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ConsentResponse>> getConsents(@RequestParam(required = false) ConsentStatus status) {
        return ResponseEntity.ok(consentService.getConsents(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConsentResponse> getConsent(@PathVariable Long id) {
        return ResponseEntity.ok(consentService.getConsent(id));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ConsentResponse> approveConsent(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(consentService.approveConsent(id, username(jwt)));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ConsentResponse> rejectConsent(@PathVariable Long id,
                                                         @Valid @RequestBody(required = false) ConsentDecisionRequest request,
                                                         @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(consentService.rejectConsent(id, reason(request), username(jwt)));
    }

    @PostMapping("/{id}/revoke")
    public ResponseEntity<ConsentResponse> revokeConsent(@PathVariable Long id,
                                                         @Valid @RequestBody(required = false) ConsentDecisionRequest request,
                                                         @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(consentService.revokeConsent(id, reason(request), username(jwt)));
    }

    // Keycloak puts the login name in preferred_username; fall back to the subject id
    private static String username(Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        return username != null ? username : jwt.getSubject();
    }

    private static String reason(ConsentDecisionRequest request) {
        return request == null ? null : request.getReason();
    }
}
