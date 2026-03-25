package com.example.repository;


import com.example.entity.TransactionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionHistory, String> {


    List<TransactionHistory> findByAccountIdOrderByCreatedAtDesc(String accountId);
}
