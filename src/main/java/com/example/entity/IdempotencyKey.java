package com.example.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "idempotency_keys")
@Builder
@AllArgsConstructor
public class IdempotencyKey {
    @Id
    @Column(name = "`key`")
    private String key;
    private String result;
    private LocalDateTime createdAt;

    public IdempotencyKey() {

    }

    @PrePersist
    public void prePersist(){
        createdAt = LocalDateTime.now();
    }
}
