package com.loyalty.loyaltyprogram.controller;

import com.loyalty.loyaltyprogram.dto.request.AddPointsRequestDto;
import com.loyalty.loyaltyprogram.dto.request.RedeemRequestDto;
import com.loyalty.loyaltyprogram.dto.request.RefundRequestDto;
import com.loyalty.loyaltyprogram.dto.response.ApiResponse;
import com.loyalty.loyaltyprogram.model.Transaction;
import com.loyalty.loyaltyprogram.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/earn")
    public ResponseEntity<ApiResponse<Transaction>> earnPoints(@Valid @RequestBody AddPointsRequestDto requestDto) {
        Transaction transaction = transactionService.earnPoints(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Points earned successfully", transaction));
    }

    @PostMapping("/redeem")
    public ResponseEntity<ApiResponse<Transaction>> redeemPoints(@Valid @RequestBody RedeemRequestDto requestDto) {
        Transaction transaction = transactionService.redeemPoints(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Points redeemed successfully", transaction));
    }

    @PostMapping("/refund")
    public ResponseEntity<ApiResponse<Transaction>> refundPoints(@Valid @RequestBody RefundRequestDto requestDto) {
        Transaction transaction = transactionService.refundPoints(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Points refunded successfully", transaction));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<Transaction>>> getTransactionsByCustomer(@PathVariable Long customerId) {
        List<Transaction> transactions = transactionService.getTransactionsByCustomer(customerId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Transactions fetched successfully", transactions));
    }
}