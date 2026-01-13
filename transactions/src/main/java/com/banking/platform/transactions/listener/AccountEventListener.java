package com.banking.platform.transactions.listener;

import com.banking.platform.transactions.entity.AccountSummary;
import com.banking.platform.transactions.events.AccountCreatedEvent;
import com.banking.platform.transactions.repository.AccountSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountEventListener {

    private final AccountSummaryRepository repository;

    @KafkaListener(topics = "banking.accounts.v1", groupId = "transactions-group")
    public void handleAccountCreated(AccountCreatedEvent event) {
        log.info("Nueva cuenta detectada ID: {}. Actualizando proyección local...", event.getAccountId());

        AccountSummary summary = AccountSummary.builder()
                .id(event.getAccountId())
                .customerId(event.getCustomerId())
                .currentBalance(event.getInitialBalance())
                .build();

        repository.save(summary);
    }
}