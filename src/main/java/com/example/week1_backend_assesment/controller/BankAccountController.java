package com.example.week1_backend_assesment.controller;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.example.week1_backend_assesment.dto.AccountRequest;
import com.example.week1_backend_assesment.entity.BankAccount;
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
    public ResponseEntity<BankAccount> createAccount(@Valid @RequestBody AccountRequest request){
        BankAccount account= bankAccountService.createBankAccount(request);
        return new ResponseEntity<>(account, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<BankAccount>> getAllAccounts(){
        List<BankAccount> accounts=bankAccountService.getAllBankAccounts();
        return ResponseEntity.ok(accounts);
    }
    @GetMapping("/{accountId}")
    public ResponseEntity<BankAccount> getAccountById(@PathVariable Long accountId){
        BankAccount account=bankAccountService.getBankAccountById(accountId);
        return ResponseEntity.ok(account);
    }
}
