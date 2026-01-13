package com.banking.platform.transactions.repository;

import com.banking.platform.transactions.entity.AccountSummary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountSummaryRepository extends JpaRepository<AccountSummary, Long> {
}
