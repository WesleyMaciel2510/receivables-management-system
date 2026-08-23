package com.rms.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "banks")
@SQLDelete(sql = "UPDATE banks SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Bank extends SoftDeletableEntity {
    @Id
    private UUID id;

    @Column(name = "compe_code", nullable = false, length = 3, unique = true)
    private String compeCode;

    @Column(nullable = false, length = 8)
    private String ispb;

    @Column(nullable = false, length = 100)
    private String name;

    protected Bank() {
    }

    public Bank(String compeCode, String ispb, String name) {
        this.id = UUID.randomUUID();
        this.compeCode = compeCode;
        this.ispb = ispb;
        this.name = name;
    }

    public UUID getId() { return id; }
    public String getCompeCode() { return compeCode; }
    public String getIspb() { return ispb; }
    public String getName() { return name; }
}
