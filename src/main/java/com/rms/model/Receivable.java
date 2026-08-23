package com.rms.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import com.rms.security.AesGcmAttributeConverter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "receivables")
@SQLDelete(sql = "UPDATE receivables SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Receivable extends SoftDeletableEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Convert(converter = AesGcmAttributeConverter.class)
    @Column(nullable = false, length = 2048)
    private String description;

    @Column(name = "original_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal originalAmount;

    @Column(name = "remaining_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal remainingBalance;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReceivableStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debtor_account_id")
    private BankAccount debtorAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creditor_account_id")
    private BankAccount creditorAccount;

    protected Receivable() {
    }

    public Receivable(Client client, String description, BigDecimal originalAmount, LocalDate dueDate) {
        this.id = UUID.randomUUID();
        this.client = client;
        this.description = description;
        this.originalAmount = originalAmount;
        this.remainingBalance = originalAmount;
        this.dueDate = dueDate;
        this.status = ReceivableStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Client getClient() { return client; }
    public String getDescription() { return description; }
    public BigDecimal getOriginalAmount() { return originalAmount; }
    public BigDecimal getRemainingBalance() { return remainingBalance; }
    public LocalDate getDueDate() { return dueDate; }
    public ReceivableStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public BankAccount getDebtorAccount() { return debtorAccount; }
    public BankAccount getCreditorAccount() { return creditorAccount; }

    public void update(String description, BigDecimal originalAmount, BigDecimal remainingBalance, LocalDate dueDate) {
        this.description = description;
        this.originalAmount = originalAmount;
        this.remainingBalance = remainingBalance;
        this.dueDate = dueDate;
    }

    public void changeStatus(ReceivableStatus status) {
        this.status = status;
    }

    public void applyPayment(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0 || amount.compareTo(remainingBalance) > 0) {
            throw new IllegalArgumentException("Payment amount exceeds receivable balance");
        }
        remainingBalance = remainingBalance.subtract(amount);
        status = remainingBalance.signum() == 0 ? ReceivableStatus.PAID : ReceivableStatus.PARTIAL;
    }
}
