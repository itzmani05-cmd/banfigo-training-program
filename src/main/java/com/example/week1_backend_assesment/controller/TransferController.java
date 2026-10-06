package com.example.week1_backend_assesment.controller;

import com.example.week1_backend_assesment.dto.TransferRequest;
import com.example.week1_backend_assesment.dto.TransferResponse;
import com.example.week1_backend_assesment.service.TransferService;
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
