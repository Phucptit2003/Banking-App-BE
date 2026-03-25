package com.example.repository;

import com.example.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {
    /**
     * PESSIMISTIC WRITE = SELECT ... FOR UPDATE
     *
     * Khi gọi method này, DB sẽ khóa row lại cho đến khi transaction kết thúc.
     * Các request khác gọi cùng accountId sẽ phải CHỜ — không thể đọc để ghi.
     *
     * Lưu ý: PHẢI nằm trong @Transactional, nếu không lock sẽ bị release ngay.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") String id);
}
