package com.banking.platform.accounts.repository;

import com.banking.platform.accounts.entity.CustomerProjection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerProjectionRepository extends JpaRepository<CustomerProjection, Long> {
}
