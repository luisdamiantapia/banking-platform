package com.banking.platform.accounts.service;

import com.banking.platform.accounts.entity.Account;
import com.banking.platform.accounts.events.AccountCreatedEvent;
import com.banking.platform.accounts.repository.AccountRepository;
import com.banking.platform.accounts.repository.CustomerProjectionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomerProjectionRepository customerProjectionRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public Account createAccount(Long customerId, BigDecimal initialCredit) {
        // 1. VALIDACIÓN LOCAL (Gracias a la proyección)
        // No hay llamada HTTP a ms-customers.
        if (!customerProjectionRepository.existsById(customerId)) {
            throw new RuntimeException("Cliente no encontrado (o evento de creación aun no procesado)");
        }

        // 2. Crear cuenta
        Account account = Account.builder()
                .customerId(customerId)
                .accountNumber(UUID.randomUUID().toString()) // Generamos numero random
                .balance(initialCredit)
                .build();

        Account savedAccount = accountRepository.save(account);

        // 3. Emitir evento (Para que ms-transactions se entere)
        AccountCreatedEvent event = AccountCreatedEvent.builder()
                .accountId(savedAccount.getId())
                .customerId(savedAccount.getCustomerId())
                .initialBalance(savedAccount.getBalance())
                .build();

        // Topic: banking.accounts.v1 (Definelo en una clase config similar a ms-customers)
        kafkaTemplate.send("banking.accounts.v1", savedAccount.getId().toString(), event);
        log.info("Cuenta creada y evento emitido: {}", savedAccount.getId());

        return savedAccount;
    }
}