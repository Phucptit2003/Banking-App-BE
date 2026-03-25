package com.example.controller;


import com.example.entity.Account;
import com.example.entity.DTOs.WithdrawDto;
import com.example.entity.TransactionHistory;
import com.example.service.WithdrawService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
@AllArgsConstructor
public class AccountController {
    private final WithdrawService withdrawService;
    @GetMapping("/{id}")
    public ResponseEntity<Account> getAccount(@PathVariable String id) {
        return ResponseEntity.ok(withdrawService.getAccount(id));
    }
    @GetMapping("/{id}/history")
    public ResponseEntity<List<TransactionHistory>> getHistory(@PathVariable String id) {
        return ResponseEntity.ok(withdrawService.getHistory(id));
    }
    @PostMapping("/withdraw/pessimistic")
    public ResponseEntity<WithdrawDto.Response> withdrawWithPessimisticLock(@RequestBody WithdrawDto.Request request, @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey){
        if (idempotencyKey == null || idempotencyKey.isBlank()){
            idempotencyKey = UUID.randomUUID().toString();
            return ResponseEntity.ok(withdrawService.withdrawWithPessimusticLock(request, idempotencyKey));
        }
        return ResponseEntity.ok(withdrawService.withdrawWithPessimusticLock(request, idempotencyKey));
    }

    @PostMapping("/withdraw/optimistic")
    public ResponseEntity<WithdrawDto.Response> withdrawOptimistic(
            @Valid @RequestBody WithdrawDto.Request request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            idempotencyKey = UUID.randomUUID().toString();
        }

        WithdrawDto.Response response = withdrawService.withdrawWithOptimisticLock(request, idempotencyKey);
        return ResponseEntity.ok(response);
    }

}
