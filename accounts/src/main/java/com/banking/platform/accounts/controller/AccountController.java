package com.banking.platform.accounts.controller;

import com.banking.platform.accounts.entity.Account;
import com.banking.platform.accounts.service.AccountService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<Account> create(@RequestBody CreateAccountRequest request) {
        return ResponseEntity.ok(accountService.createAccount(request.getCustomerId(), request.getInitialBalance()));
    }

    @Data
    public static class CreateAccountRequest {
        private Long customerId;
        private BigDecimal initialBalance;
    }
}