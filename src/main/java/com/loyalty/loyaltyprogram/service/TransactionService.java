package com.loyalty.loyaltyprogram.service;

import com.loyalty.loyaltyprogram.dto.request.AddPointsRequestDto;
import com.loyalty.loyaltyprogram.dto.request.RedeemRequestDto;
import com.loyalty.loyaltyprogram.dto.request.RefundRequestDto;
import com.loyalty.loyaltyprogram.model.Transaction;

import java.util.List;

public interface TransactionService {
    Transaction earnPoints(AddPointsRequestDto requestDto);
    Transaction redeemPoints(RedeemRequestDto requestDto);
    Transaction refundPoints(RefundRequestDto requestDto);
    List<Transaction> getTransactionsByCustomer(Long customerId);
}