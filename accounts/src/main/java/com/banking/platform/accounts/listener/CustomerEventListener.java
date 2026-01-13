package com.banking.platform.accounts.listener;

import com.banking.platform.accounts.entity.Account;
import com.banking.platform.accounts.entity.CustomerProjection;
import com.banking.platform.accounts.events.CustomerCreatedEvent;
import com.banking.platform.accounts.events.TransactionCreatedEvent;
import com.banking.platform.accounts.repository.AccountRepository;
import com.banking.platform.accounts.repository.CustomerProjectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomerEventListener {

    private final CustomerProjectionRepository repository;
    private final AccountRepository accountRepository;

    // Escuchamos el topic definido en ms-customers
    @KafkaListener(
            topics = "banking.customers.v1",
            groupId = "accounts-group",
            properties = {
                    "spring.json.value.default.type: com.banking.platform.accounts.events.CustomerCreatedEvent"
            }
    )
    public void handleCustomerCreated(CustomerCreatedEvent event) {
        log.info("Evento recibido en ms-accounts: Cliente creado ID {}", event.getId());

        // Idempotencia: save() en JPA hace update si el ID ya existe, insert si no.
        CustomerProjection projection = CustomerProjection.builder()
                .id(event.getId())
                .fullName(event.getFullName())
                .email(event.getEmail())
                .build();

        repository.save(projection);
        log.info("Proyección actualizada para Cliente ID {}", event.getId());
    }

    @KafkaListener(
            topics = "banking.transactions.v1",
            groupId = "accounts-group",
            properties = {
                    "spring.json.value.default.type: com.banking.platform.accounts.events.TransactionCreatedEvent"
            }
    )
    public void handleTransaction(TransactionCreatedEvent event) {
        log.info("Procesando transacción ID {} en Cuentas Oficiales", event.getTransactionId());

        Account origin = accountRepository.findById(event.getFromAccountId()).orElseThrow();
        Account dest = accountRepository.findById(event.getToAccountId()).orElseThrow();

        origin.setBalance(origin.getBalance().subtract(event.getAmount()));
        dest.setBalance(dest.getBalance().add(event.getAmount()));

        accountRepository.save(origin);
        accountRepository.save(dest);

        log.info("Saldos oficiales actualizados para cuentas {} current balance {} y detino {} current balance {}", origin.getId(), origin.getBalance(), dest.getId(), dest.getBalance());

        // Nivel DIOS (Opcional): Aquí ms-accounts emitiría un 'BalanceUpdatedEvent'
        // para que ms-transactions corrija su proyección si hubo algún desfase.
    }
}