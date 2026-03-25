package com.example.exception;

public class InsufficientBalanceException extends RuntimeException{
    public InsufficientBalanceException(double balance, double amount) {
        super("Insufficient balance: " + balance + " < " + amount);
    }
}
