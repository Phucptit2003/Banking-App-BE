package com.example.entity.DTOs;

import lombok.Data;

public class WithdrawDto {
    @Data
    public static class Request{
        private String accountId;
        private double amount;
    }

    @Data
    public static class Response{
        private String transactionId;
        private double balanceAfter;
        private String message;
        private boolean success;

        public static Response success(String transactionId, double balanceAfter){
            Response response = new Response();
            response.success= true;
            response.transactionId = transactionId;
            response.balanceAfter = balanceAfter;
            response.message = "Withdraw successful";
            return response;
        }
    }
}
