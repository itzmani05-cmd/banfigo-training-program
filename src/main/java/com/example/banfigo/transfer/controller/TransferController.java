package com.example.banfigo.transfer.controller;

import com.example.banfigo.transfer.dto.TransferRequest;
import com.example.banfigo.transfer.dto.TransferResponse;
import com.example.banfigo.transfer.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    // Clients should send a unique Idempotency-Key per transfer (e.g. a UUID) and reuse it when retrying,
    // so a retried or double-submitted request doesn't move the money twice
    @PostMapping
    public ResponseEntity<TransferResponse> createTransfer(
            @Valid @RequestBody TransferRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        String key = idempotencyKey == null || idempotencyKey.isBlank() ? null : idempotencyKey.trim();
        if (key != null && key.length() > 100) {
            throw new IllegalArgumentException("Idempotency-Key cannot be longer than 100 characters");
        }

        TransferResponse response;
        try {
            response = transferService.transfer(request, key);
        } catch (DataIntegrityViolationException ex) {
            // Two requests with the same key arrived together and the other one made the transfer
            if (key == null) {
                throw ex;
            }
            response = transferService.replayAfterConflict(key, request).orElseThrow(() -> ex);
        }
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
