package com.example.service;


import com.example.entity.Account;
import com.example.entity.DTOs.WithdrawDto;
import com.example.entity.IdempotencyKey;
import com.example.entity.TransactionHistory;
import com.example.exception.AccountNotFoundException;
import com.example.exception.InsufficientBalanceException;
import com.example.repository.AccountRepository;
import com.example.repository.IdempotencyRepository;
import com.example.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

@Slf4j
@Service
@AllArgsConstructor
public class WithdrawService {
    private final AccountRepository accountRepository;
    private final IdempotencyRepository idempotencyKeyRepository;
    private final TransactionRepository transactionRepository;
    private final ObjectMapper objectMapper;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public WithdrawDto.Response withdrawWithPessimusticLock(WithdrawDto.Request request, String idempotencyKey){
        log.info("[PESSIMISTIC] Bắt đầu rút tiền. Account={}, Amount={}, Key={}",
                request.getAccountId(), request.getAmount(), idempotencyKey);
        // ---- BƯỚC 1: Kiểm tra Idempotency Key ----
        Optional<IdempotencyKey> idempotencyKeyOpt = idempotencyKeyRepository.findById(idempotencyKey);
        if(idempotencyKeyOpt.isPresent()){
            log.warn("[PESSIMISTIC] Key đã tồn tại, trả về kết quả cũ. Key={}", idempotencyKey);
            return parseResponse(idempotencyKeyOpt.get().getResult());
        }

        // ---- BƯỚC 2: Lock row Account (SELECT FOR UPDATE) ----
        // Từ đây cho đến khi transaction kết thúc, không ai đọc-để-ghi được row này
        Account account = accountRepository.findByIdForUpdate(request.getAccountId()).orElseThrow(() -> new AccountNotFoundException(request.getAccountId()));
        log.debug("[PESSIMISTIC] Đã lock account. ID={}, Balance={}", account.getId(), account.getBalance());
        // ---- BƯỚC 3: Kiểm tra số dư ----
        if(account.getBalance() < request.getAmount()){
            throw new InsufficientBalanceException(account.getBalance(), request.getAmount());
        }
        // ---- BƯỚC 4: Trừ tiền ----
        double balanceBefore = account.getBalance();
        account.setBalance(account.getBalance()- request.getAmount());
        accountRepository.save(account);

        // ---- BƯỚC 5: Lưu lịch sử giao dịch ----
        String txId = UUID.randomUUID().toString();
        transactionRepository.save(TransactionHistory.builder()
                        .accountId(request.getAccountId())
                        .amount(request.getAmount())
                        .balanceBefore(balanceBefore)
                        .balanceAfter(account.getBalance())
                        .type("WITHDRAW")
                        .idempotencyKey(idempotencyKey)
                        .note("WITHDRAW (PESSIMISTIC LOCK)")
                .build());
        // ---- BƯỚC 6: Lưu Idempotency Key ----
        WithdrawDto.Response response = WithdrawDto.Response.success(txId, account.getBalance());
        idempotencyKeyRepository.save(IdempotencyKey.builder().key(idempotencyKey).result(toJson(response)).build());
        log.info("[PESSIMISTIC] Thành công. Account={}, Before={}, After={}",
                account.getId(), balanceBefore, account.getBalance());
        return response;


    }
    // =========================================================
    // GIẢI PHÁP 2: OPTIMISTIC LOCK + RETRY
    // =========================================================

    /**
     * Rút tiền với Optimistic Lock.
     *
     * Không khóa DB row. Thay vào đó:
     * - Đọc record kèm @Version (ví dụ version=3)
     * - UPDATE sẽ thêm điều kiện: WHERE id=? AND version=3
     * - Nếu lúc này có request khác đã UPDATE trước (version đã thành 4),
     *   Hibernate ném ObjectOptimisticLockingFailureException
     * - @Retryable tự động thử lại tối đa 3 lần
     *
     * Phù hợp hơn khi: ít xảy ra xung đột, cần throughput cao.
     */
    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2)
    )
    @Transactional
    public WithdrawDto.Response withdrawWithOptimisticLock(
            WithdrawDto.Request request,
            String idempotencyKey) {

        log.info("[OPTIMISTIC] Bắt đầu rút tiền. Account={}, Amount={}", request.getAccountId(), request.getAmount());

        // Kiểm tra Idempotency Key
        Optional<IdempotencyKey> existingKey = idempotencyKeyRepository.findById(idempotencyKey);
        if (existingKey.isPresent()) {
            return parseResponse(existingKey.get().getResult());
        }

        // Đọc account (KHÔNG lock, chỉ đọc version)
        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new AccountNotFoundException(request.getAccountId()));

        if (account.getBalance() < request.getAmount()) {
            throw new InsufficientBalanceException(account.getBalance(), request.getAmount());
        }

        double balanceBefore = account.getBalance();
        account.setBalance(account.getBalance() - request.getAmount());

        // Nếu version trong DB khác với lúc đọc → ném exception → @Retryable tự retry
        accountRepository.save(account);

        String txId = UUID.randomUUID().toString();
        transactionRepository.save(TransactionHistory.builder()
                .accountId(account.getId())
                .amount(request.getAmount())
                .balanceBefore(balanceBefore)
                .balanceAfter(account.getBalance())
                .type("WITHDRAW")
                .idempotencyKey(idempotencyKey)
                .note("Rút tiền (Optimistic Lock)")
                .build());

        WithdrawDto.Response response = WithdrawDto.Response.success(txId, account.getBalance());
        idempotencyKeyRepository.save(IdempotencyKey.builder()
                .key(idempotencyKey)
                .result(toJson(response))
                .build());

        log.info("[OPTIMISTIC] Thành công. Account={}, Before={}, After={}",
                account.getId(), balanceBefore, account.getBalance());

        return response;
    }


    public Account getAccount(String accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    public List<TransactionHistory> getHistory(String accountId) {
        return transactionRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
    }

    private String toJson(WithdrawDto.Response response){
        try {
            return objectMapper.writeValueAsString(response);
        }
        catch (Exception e){
            throw new RuntimeException("Parse response error", e);
        }
    }

    private WithdrawDto.Response parseResponse(String json){
        try {
            return objectMapper.readValue(json, WithdrawDto.Response.class);
        }
        catch (Exception e){
            throw  new RuntimeException("Parse response error", e);
        }
    }

}
