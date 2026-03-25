package com.example.service;


import com.example.entity.Account;
import com.example.entity.DTOs.WithdrawDto;
import com.example.repository.AccountRepository;
import lombok.AllArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class WithdrawServiceTest {
    @Autowired
    private WithdrawService withdrawService;
    @Autowired
    private AccountRepository accountRepository;
    private static final String ACCOUNT_ID = "TEST001";
    @BeforeEach
    void setup(){
        accountRepository.findById(ACCOUNT_ID).ifPresentOrElse(
                account ->{
                    account.setBalance(100000000);
                    accountRepository.save(account);
                },
                () -> accountRepository.save(Account.builder().id(ACCOUNT_ID).ownerName("TEST USER").balance(100000000).build())
        );
    }

    @Test
    @DisplayName("Cùng Idempotency-Key gửi 2 lần → chỉ trừ tiền 1 lần")
    void testIdempotency_sameKeyShouldWithdrawOnce() {
        String sameKey = UUID.randomUUID().toString();
        WithdrawDto.Request req = new WithdrawDto.Request();
        req.setAccountId(ACCOUNT_ID);
        req.setAmount(2000000);

        // Gửi lần 1
        WithdrawDto.Response res1 = withdrawService.withdrawWithPessimusticLock(req, sameKey);
        // Gửi lần 2 (giả lập double-click)
        WithdrawDto.Response res2 = withdrawService.withdrawWithPessimusticLock(req, sameKey);

        // Kết quả phải giống nhau
        assertEquals(res1.getBalanceAfter(), res2.getBalanceAfter(), "Kết quả lần 2 phải bằng lần 1");

        // Số dư thực tế chỉ bị trừ 1 lần
        Account account = accountRepository.findById(ACCOUNT_ID).orElseThrow();
        assertEquals(98000000, account.getBalance(), "Số dư phải là 800k (trừ 1 lần 200k)");

        System.out.println("✅ Idempotency OK. Số dư cuối: " + account.getBalance());
    }

    // ============================================================
    // TEST 2: Race condition — 10 request đồng thời chỉ trừ đúng số lần
    // ============================================================
    @Test
    @DisplayName("10 request đồng thời rút 1000000 VND → tổng trừ tối đa 10,000,000 VND")
    void testConcurrentWithdraw_pessimisticLock() throws InterruptedException {
        int threadCount = 10;
        double withdrawAmount = 1000000;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                try {
                    latch.await(); // chờ tất cả sẵn sàng
                    WithdrawDto.Request req = new WithdrawDto.Request();
                    req.setAccountId(ACCOUNT_ID);
                    req.setAmount(withdrawAmount);
                    withdrawService.withdrawWithPessimusticLock(req, UUID.randomUUID().toString());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.out.println("Request thất bại (expected): " + e.getMessage());
                }
            }));
        }

        latch.countDown(); // Tất cả thread bắt đầu cùng lúc!
        for (Future<?> f : futures) {
            try { f.get(); } catch (Exception ignored) {}
        }
        executor.shutdown();

        Account account = accountRepository.findById(ACCOUNT_ID).orElseThrow();
        double expectedBalance = 100000000 - (successCount.get() * withdrawAmount);

        System.out.printf("✅ Kết quả: %d thành công, %d thất bại%n", successCount.get(), failCount.get());
        System.out.printf("   Số dư ban đầu: 100,000,000 | Số dư cuối: %f %n", account.getBalance());

        // Số dư KHÔNG ĐƯỢC âm
        assertTrue(account.getBalance() >= 0, "Số dư KHÔNG được âm!");
        assertEquals(expectedBalance, account.getBalance(), "Số dư không khớp với số lần trừ!");
    }
}
