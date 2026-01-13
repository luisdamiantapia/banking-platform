package com.banking.platform.transactions.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccountCreatedEvent {
    private Long accountId;
    private Long customerId;
    private BigDecimal initialBalance;
}