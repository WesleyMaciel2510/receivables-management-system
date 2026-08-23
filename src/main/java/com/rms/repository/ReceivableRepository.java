package com.rms.repository;

import com.rms.model.Receivable;
import java.util.UUID;
import java.util.List;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReceivableRepository extends JpaRepository<Receivable, UUID> {
    boolean existsByClientId(UUID clientId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select receivable from Receivable receivable where receivable.id = :id")
    java.util.Optional<Receivable> findByIdForUpdate(@Param("id") UUID id);

    @Query(value = "SELECT * FROM receivables WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC", nativeQuery = true)
    List<Receivable> findArchived();
}
