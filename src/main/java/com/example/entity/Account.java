package com.example.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Account {
    @Id
    private String id;
    private String ownerName;
    private double balance;

    /**
     * @Version dùng cho Optimistic Lock.
     * Hibernate tự tăng mỗi lần UPDATE.
     * Nếu 2 request cùng đọc version=1, request nào UPDATE sau sẽ bị lỗi
     * vì version trong DB đã là 2 rồi.
     */
    @Version
    private int version;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersit(){
    createdAt = LocalDateTime.now();
     updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate(){
        updatedAt = LocalDateTime.now();
    }
}
