package com.rms.repository;

import com.rms.model.Bank;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankRepository extends JpaRepository<Bank, UUID> {
    List<Bank> findAllByOrderByCompeCodeAsc();
    Optional<Bank> findByCompeCode(String compeCode);
}
