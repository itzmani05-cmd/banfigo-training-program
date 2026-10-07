package com.example.banfigo.transfer.controller;

import com.example.banfigo.transfer.dto.TransferRequest;
import com.example.banfigo.transfer.dto.TransferResponse;
import com.example.banfigo.transfer.service.TransferService;
import jakarta.validation.Valid;
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

    @PostMapping
    public ResponseEntity<TransferResponse> createTransfer(
            @Valid @RequestBody TransferRequest request) {

        return new ResponseEntity<>(transferService.transfer(request), HttpStatus.CREATED);
    }
}
