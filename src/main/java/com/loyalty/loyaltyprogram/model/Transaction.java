package com.loyalty.loyaltyprogram.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.loyalty.loyaltyprogram.enums.TransactionStatus;
import com.loyalty.loyaltyprogram.enums.TransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private Double points;

    // Generated when points are earned
    @Column(name = "earn_txn_id")
    private String earnTxnId;

    // Generated during redemption, or referenced during refund
    @Column(name = "redeem_txn_id")
    private String redeemTxnId;

    // Generated only when a refund occurs
    @Column(name = "refund_txn_id")
    private String refundTxnId;

    @PrePersist
    public void generateTransactionIds() {
        if (this.transactionType == TransactionType.EARNED && this.earnTxnId == null) {
            this.earnTxnId = UUID.randomUUID().toString();
        } else if (this.transactionType == TransactionType.REDEEM && this.redeemTxnId == null) {
            this.redeemTxnId = UUID.randomUUID().toString();
        } else if (this.transactionType == TransactionType.REFUND && this.refundTxnId == null) {
            this.refundTxnId = UUID.randomUUID().toString();
        }
    }
}