package com.banking.platform.accounts.events;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AccountCreatedEvent {
    private Long accountId;
    private Long customerId;
    private BigDecimal initialBalance;
}