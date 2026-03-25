package com.example.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_history")
@Data
@Builder
@AllArgsConstructor
public class TransactionHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String accountId;
    private double amount;
    private double balanceBefore;
    private double balanceAfter;
    private String type;
    private String idempotencyKey;
    private String note;
    private LocalDateTime createdAt;

    public TransactionHistory() {

    }

    @PrePersist
    public void prePresist(){
        createdAt = LocalDateTime.now();
    }
}
