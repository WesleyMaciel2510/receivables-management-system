package com.rms.repository;

import com.rms.model.AccountType;
import com.rms.model.BankAccount;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface BankAccountRepository extends JpaRepository<BankAccount, UUID> {
    List<BankAccount> findAllByClientIdOrderByAccountTypeAsc(UUID clientId);
    boolean existsByClientIdAndBankIdAndAccountType(UUID clientId, UUID bankId, AccountType accountType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select account from BankAccount account where account.id = :id")
    java.util.Optional<BankAccount> findByIdForUpdate(@Param("id") UUID id);
}
