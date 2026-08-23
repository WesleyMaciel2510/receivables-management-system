package com.rms.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import com.rms.security.AesGcmAttributeConverter;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "clients")
@SQLDelete(sql = "UPDATE clients SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Client extends SoftDeletableEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 160)
    private String name;

    @Convert(converter = AesGcmAttributeConverter.class)
    @Column(name = "cpf_cnpj", nullable = false, length = 1024)
    private String cpfCnpj;

    @Convert(converter = AesGcmAttributeConverter.class)
    @Column(nullable = false, length = 1024)
    private String email;

    @Convert(converter = AesGcmAttributeConverter.class)
    @Column(nullable = false, length = 1024)
    private String phone;

    @Column(name = "cpf_cnpj_hash", length = 64)
    private String cpfCnpjHash;

    @Column(name = "email_hash", length = 64)
    private String emailHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ClientStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Client() {
    }

    public Client(String name, String cpfCnpj, String email, String phone, ClientStatus status) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.cpfCnpj = cpfCnpj;
        this.email = email;
        this.phone = phone;
        this.status = status;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getCpfCnpj() { return cpfCnpj; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getCpfCnpjHash() { return cpfCnpjHash; }
    public String getEmailHash() { return emailHash; }
    public ClientStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }

    public void updateSearchIndexes(String cpfCnpjHash, String emailHash) {
        this.cpfCnpjHash = cpfCnpjHash;
        this.emailHash = emailHash;
    }

    public void update(String name, String cpfCnpj, String email, String phone, ClientStatus status) {
        this.name = name;
        this.cpfCnpj = cpfCnpj;
        this.email = email;
        this.phone = phone;
        this.status = status;
    }
}
