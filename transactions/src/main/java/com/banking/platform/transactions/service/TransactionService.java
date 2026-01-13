package com.banking.platform.transactions.service;

import com.banking.platform.transactions.entity.AccountSummary;
import com.banking.platform.transactions.entity.Transaction;
import com.banking.platform.transactions.events.TransactionCreatedEvent;
import com.banking.platform.transactions.repository.AccountSummaryRepository;
import com.banking.platform.transactions.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountSummaryRepository accountSummaryRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public Transaction transfer(Long fromId, Long toId, BigDecimal amount) {
        // 1. VALIDACIÓN CON PROYECCIÓN LOCAL (Cero latencia de red)
        AccountSummary origin = accountSummaryRepository.findById(fromId)
                .orElseThrow(() -> new RuntimeException("Cuenta origen no existe en proyección"));

        AccountSummary destination = accountSummaryRepository.findById(toId)
                .orElseThrow(() -> new RuntimeException("Cuenta destino no existe en proyección"));

        if (origin.getCurrentBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Saldo insuficiente (según proyección local)");
        }

        // 2. Guardar Transacción
        Transaction tx = Transaction.builder()
                .fromAccountId(fromId)
                .toAccountId(toId)
                .amount(amount)
                .timestamp(LocalDateTime.now())
                .status("PENDING_PROCESSING") // El estado final lo confirmaría un evento de vuelta
                .build();

        transactionRepository.save(tx);

        // 3. ACTUALIZAR PROYECCIÓN LOCAL (Optimistic Update)
        // Restamos saldo inmediatamente en local para evitar doble gasto antes de que ms-accounts confirme
        origin.setCurrentBalance(origin.getCurrentBalance().subtract(amount));
        destination.setCurrentBalance(destination.getCurrentBalance().add(amount));
        accountSummaryRepository.save(origin);
        accountSummaryRepository.save(destination);

        // 4. EMITIR EVENTO (Para que ms-accounts actualice el ledger oficial)
        TransactionCreatedEvent event = TransactionCreatedEvent.builder()
                .transactionId(tx.getId())
                .fromAccountId(fromId)
                .toAccountId(toId)
                .amount(amount)
                .build();

        kafkaTemplate.send("banking.transactions.v1", tx.getId().toString(), event);
        log.info("Transferencia iniciada: {}", tx.getId());

        return tx;
    }
}