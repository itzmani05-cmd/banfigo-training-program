package com.example.week1_backend_assesment.controller;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.example.week1_backend_assesment.dto.AccountRequest;
import com.example.week1_backend_assesment.dto.AccountResponse;
import com.example.week1_backend_assesment.service.BankAccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class BankAccountController {
    private final BankAccountService bankAccountService;
    public BankAccountController(BankAccountService bankAccountService){
        this.bankAccountService=bankAccountService;
    }
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody AccountRequest request){
        AccountResponse account= bankAccountService.createBankAccount(request);
        return new ResponseEntity<>(account, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> getAllAccounts(){
        List<AccountResponse> accounts=bankAccountService.getAllBankAccounts();
        return ResponseEntity.ok(accounts);
    }
    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccountById(@PathVariable Long accountId){
        AccountResponse account=bankAccountService.getBankAccountById(accountId);
        return ResponseEntity.ok(account);
    }
}
