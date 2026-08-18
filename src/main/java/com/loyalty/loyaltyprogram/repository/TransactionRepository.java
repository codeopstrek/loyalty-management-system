package com.loyalty.loyaltyprogram.repository;

import com.loyalty.loyaltyprogram.enums.TransactionStatus;
import com.loyalty.loyaltyprogram.enums.TransactionType;
import com.loyalty.loyaltyprogram.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByCustomerId(Long customerId);

    List<Transaction> findByEarnTxnId(String earnTxnId);

    List<Transaction> findByRedeemTxnId(String redeemTxnId);

    List<Transaction> findByRedeemTxnIdAndCustomerId(String redeemTxnId, Long customerId);

    List<Transaction> findByRefundTxnId(String refundTxnId);

    List<Transaction> findByRedeemTxnIdAndTransactionTypeAndStatus(
            String redeemTxnId, 
            TransactionType transactionType, 
            TransactionStatus status
    );
}