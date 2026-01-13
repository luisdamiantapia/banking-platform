package com.banking.platform.transactions.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "accounts_summary_view")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccountSummary {
    @Id
    private Long id; // El ID original de ms-accounts
    private Long customerId;
    private BigDecimal currentBalance;
}