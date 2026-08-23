package com.rms.repository;

import com.rms.model.PaymentEffect;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentEffectRepository extends JpaRepository<PaymentEffect, UUID> {
    boolean existsByEventId(UUID eventId);
}
