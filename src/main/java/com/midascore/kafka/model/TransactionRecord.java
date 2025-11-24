package com.midascore.kafka.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "transaction_records")
public class TransactionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sender;

    @Column(nullable = false)
    private String recipient;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal incentive;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    public TransactionRecord() {
    }

    public TransactionRecord(String sender, String recipient, BigDecimal amount, BigDecimal incentive, OffsetDateTime createdAt) {
        this.sender = sender;
        this.recipient = recipient;
        this.amount = amount;
        this.incentive = incentive;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getSender() {
        return sender;
    }

    public String getRecipient() {
        return recipient;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getIncentive() {
        return incentive;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
