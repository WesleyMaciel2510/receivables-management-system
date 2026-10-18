package com.rms.service;

import com.rms.model.BankAccount;
import com.rms.model.PaymentEffect;
import com.rms.model.Receivable;
import com.rms.repository.BankAccountRepository;
import com.rms.repository.PaymentEffectRepository;
import com.rms.repository.ReceivableRepository;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentReconciliationService {
    private final BankAccountRepository accountRepository;
    private final ReceivableRepository receivableRepository;
    private final PaymentEffectRepository paymentEffectRepository;

    public PaymentReconciliationService(BankAccountRepository accountRepository,
            ReceivableRepository receivableRepository, PaymentEffectRepository paymentEffectRepository) {
        this.accountRepository = accountRepository;
        this.receivableRepository = receivableRepository;
        this.paymentEffectRepository = paymentEffectRepository;
    }

    @Transactional
    public void reconcile(PaymentPublisher.PaymentEvent event) {
        if (paymentEffectRepository.existsByEventId(event.eventId())) return;
        Receivable receivable = receivableRepository.findByIdForUpdate(event.receivableId())
                .orElseThrow(() -> new ResourceNotFoundException("Receivable not found: " + event.receivableId()));
        List<UUID> ids = List.of(event.debtorAccountId(), event.creditorAccountId()).stream()
                .distinct().sorted(Comparator.comparing(UUID::toString)).toList();
        List<BankAccount> locked = ids.stream().map(id -> accountRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bank account not found: " + id))).toList();
        BankAccount debtor = locked.stream().filter(account -> account.getId().equals(event.debtorAccountId()))
                .findFirst().orElseThrow();
        BankAccount creditor = locked.stream().filter(account -> account.getId().equals(event.creditorAccountId()))
                .findFirst().orElseThrow();
        if (!debtor.getId().equals(creditor.getId())) {
            debtor.debit(event.amount());
            creditor.credit(event.amount());
        }
        receivable.applyPayment(event.amount());
        paymentEffectRepository.saveAndFlush(new PaymentEffect(event.eventId()));
    }
}
