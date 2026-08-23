package com.rms.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "bank_accounts")
@SQLDelete(sql = "UPDATE bank_accounts SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class BankAccount extends SoftDeletableEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "bank_id", nullable = false)
    private Bank bank;

    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;

    @Column(nullable = false, length = 10)
    private String agency;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 32)
    private AccountType accountType;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(name = "monthly_maintenance_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyMaintenanceFee;

    protected BankAccount() {
    }

    public BankAccount(Client client, Bank bank, String accountNumber, String agency,
            AccountType accountType, BigDecimal monthlyMaintenanceFee) {
        this.id = UUID.randomUUID();
        this.client = client;
        this.bank = bank;
        this.accountNumber = accountNumber;
        this.agency = agency;
        this.accountType = accountType;
        this.balance = BigDecimal.ZERO.setScale(4);
        this.monthlyMaintenanceFee = normalizeFee(accountType, monthlyMaintenanceFee);
    }

    private static BigDecimal normalizeFee(AccountType type, BigDecimal fee) {
        BigDecimal value = fee == null ? BigDecimal.ZERO : fee;
        if (value.signum() < 0) {
            throw new IllegalArgumentException("Monthly maintenance fee cannot be negative");
        }
        if (type == AccountType.PAYMENT && value.signum() != 0) {
            throw new IllegalArgumentException("Payment accounts must have zero maintenance fee");
        }
        return value.setScale(2);
    }

    public UUID getId() { return id; }
    public Client getClient() { return client; }
    public Bank getBank() { return bank; }
    public String getAccountNumber() { return accountNumber; }
    public String getAgency() { return agency; }
    public AccountType getAccountType() { return accountType; }
    public BigDecimal getBalance() { return balance; }
    public BigDecimal getMonthlyMaintenanceFee() { return monthlyMaintenanceFee; }

    public void debit(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("Debit amount must be positive");
        if (balance.compareTo(amount) < 0) throw new IllegalArgumentException("Insufficient account balance");
        balance = balance.subtract(amount).setScale(4);
    }

    public void credit(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("Credit amount must be positive");
        balance = balance.add(amount).setScale(4);
    }
}
