package com.loyalty.loyaltyprogram.service.impl;

import com.loyalty.loyaltyprogram.dto.request.AddPointsRequestDto;
import com.loyalty.loyaltyprogram.dto.request.RedeemRequestDto;
import com.loyalty.loyaltyprogram.dto.request.RefundRequestDto;
import com.loyalty.loyaltyprogram.enums.AccountStatus;
import com.loyalty.loyaltyprogram.enums.TransactionStatus;
import com.loyalty.loyaltyprogram.enums.TransactionType;
import com.loyalty.loyaltyprogram.exception.CustomerAccountDeactivatedException;
import com.loyalty.loyaltyprogram.exception.DuplicateResourceException;
import com.loyalty.loyaltyprogram.exception.PointsNotAvailableException;
import com.loyalty.loyaltyprogram.exception.TransactionNotFoundException;
import com.loyalty.loyaltyprogram.model.Customer;
import com.loyalty.loyaltyprogram.model.Transaction;
import com.loyalty.loyaltyprogram.repository.CustomerRepository;
import com.loyalty.loyaltyprogram.repository.TransactionRepository;
import com.loyalty.loyaltyprogram.service.CustomerService;
import com.loyalty.loyaltyprogram.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;
    private final CustomerService customerService;

    @Override
    @Transactional
    public Transaction earnPoints(AddPointsRequestDto requestDto) {
        log.info("Initiating EARN transaction process | customerId: {}, points: {}", 
                requestDto.getCustomerId(), requestDto.getPoints());

        Customer customer = customerService.getCustomerById(requestDto.getCustomerId());
        validateActive(customer);

        log.info("Customer verified and active | customerId: {}, currentBalance: {}", 
                customer.getCustomerId(), customer.getRedeemablePoints());

        double previousPoints = customer.getRedeemablePoints();
        customer.setRedeemablePoints(previousPoints + requestDto.getPoints());
        customerRepository.save(customer);
        
        log.info("Customer points credited | customerId: {}, previousBalance: {}, newBalance: {}", 
                customer.getCustomerId(), previousPoints, customer.getRedeemablePoints());

        Transaction transaction = Transaction.builder()
                .customerId(customer.getCustomerId())
                .transactionType(TransactionType.EARNED)
                .status(TransactionStatus.SUCCESS)
                .timestamp(LocalDateTime.now())
                .points(requestDto.getPoints())
                .earnTxnId(UUID.randomUUID().toString())
                .build();

        Transaction saved = transactionRepository.save(transaction);
        log.info("EARN transaction completed successfully | earnTxnId: {}, customerId: {}, pointsEarned: {}", 
                saved.getEarnTxnId(), customer.getCustomerId(), saved.getPoints());
        return saved;
    }

    @Override
    @Transactional
    public Transaction redeemPoints(RedeemRequestDto requestDto) {
        log.info("Initiating REDEEM transaction process | customerId: {}, points: {}", 
                requestDto.getCustomerId(), requestDto.getPoints());

        Customer customer = customerService.getCustomerById(requestDto.getCustomerId());
        validateActive(customer);

        log.info("Customer verified and active | customerId: {}, currentBalance: {}", 
                customer.getCustomerId(), customer.getRedeemablePoints());

        if (customer.getRedeemablePoints() < requestDto.getPoints()) {
            log.warn("REDEEM transaction failed: Insufficient points | customerId: {}, available: {}, requested: {}", 
                    customer.getCustomerId(), customer.getRedeemablePoints(), requestDto.getPoints());

            Transaction failedTxn = transactionRepository.save(
                    Transaction.builder()
                            .customerId(customer.getCustomerId())
                            .transactionType(TransactionType.REDEEM)
                            .status(TransactionStatus.FAILURE)
                            .timestamp(LocalDateTime.now())
                            .points(requestDto.getPoints())
                            .redeemTxnId(UUID.randomUUID().toString())
                            .build()
            );
            log.info("Failed REDEEM audit record created | redeemTxnId: {}, customerId: {}", 
                    failedTxn.getRedeemTxnId(), customer.getCustomerId());

            throw new PointsNotAvailableException("Insufficient points for customer id: " + customer.getCustomerId());
        }

        double previousPoints = customer.getRedeemablePoints();
        customer.setRedeemablePoints(previousPoints - requestDto.getPoints());
        customerRepository.save(customer);
        
        log.info("Customer points debited | customerId: {}, previousBalance: {}, newBalance: {}", 
                customer.getCustomerId(), previousPoints, customer.getRedeemablePoints());

        Transaction transaction = Transaction.builder()
                .customerId(customer.getCustomerId())
                .transactionType(TransactionType.REDEEM)
                .status(TransactionStatus.SUCCESS)
                .timestamp(LocalDateTime.now())
                .points(requestDto.getPoints())
                .redeemTxnId(UUID.randomUUID().toString())
                .build();

        Transaction saved = transactionRepository.save(transaction);
        log.info("REDEEM transaction completed successfully | redeemTxnId: {}, customerId: {}, pointsRedeemed: {}", 
                saved.getRedeemTxnId(), customer.getCustomerId(), saved.getPoints());
        return saved;
    }

    @Override
    @Transactional
    public Transaction refundPoints(RefundRequestDto requestDto) {
        log.info("Initiating refund process | customerId: {}, redeemTxnId: {}",
                requestDto.getCustomerId(), requestDto.getRedeemTxnId());

        Customer customer = customerService.getCustomerById(requestDto.getCustomerId());
        validateActive(customer);

        log.info("Customer verified and active | customerId: {}, currentBalance: {}", 
                customer.getCustomerId(), customer.getRedeemablePoints());

        // Single DB call to fetch all transactions linked to this redeemTxnId
        List<Transaction> relatedTxns = transactionRepository
                .findByRedeemTxnIdAndCustomerId(requestDto.getRedeemTxnId(), requestDto.getCustomerId());

        // Verify original successful redemption
        Transaction originalRedeemTxn = relatedTxns.stream()
                .filter(t -> t.getTransactionType() == TransactionType.REDEEM && t.getStatus() == TransactionStatus.SUCCESS)
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("Refund failed: Original redemption not found | customerId: {}, redeemTxnId: {}",
                            requestDto.getCustomerId(), requestDto.getRedeemTxnId());
                    saveFailedRefundAudit(customer.getCustomerId(), requestDto.getRedeemTxnId(), 0.0);
                    return new TransactionNotFoundException("Redeem transaction not found with id: "
                            + requestDto.getRedeemTxnId() + " for customer id: " + requestDto.getCustomerId());
                });

        double pointsToRefund = originalRedeemTxn.getPoints();
        log.info("Original redemption transaction verified | redeemTxnId: {}, pointsToRefund: {}",
                requestDto.getRedeemTxnId(), pointsToRefund);

        // Extract the existing successful refund if it exists
        Optional<Transaction> existingRefund = relatedTxns.stream()
                .filter(t -> t.getTransactionType() == TransactionType.REFUND && t.getStatus() == TransactionStatus.SUCCESS)
                .findFirst();

        if (existingRefund.isPresent()) {
            String existingRefundTxnId = existingRefund.get().getRefundTxnId();

            log.warn("Refund failed: Duplicate refund request | customerId: {}, redeemTxnId: {}, existingRefundTxnId: {}", 
                    requestDto.getCustomerId(), requestDto.getRedeemTxnId(), existingRefundTxnId);

            saveFailedRefundAudit(customer.getCustomerId(), requestDto.getRedeemTxnId(), pointsToRefund);

            throw new DuplicateResourceException(
                    "Refund already initiated for redeemTxnId: " + requestDto.getRedeemTxnId() 
                    + " (existing refundTxnId: " + existingRefundTxnId + ")", "redeemTxnId");
        }

        log.info("Refund validations passed | customerId: {}, redeemTxnId: {}", 
                requestDto.getCustomerId(), requestDto.getRedeemTxnId());

        // Credit points back
        double previousPoints = customer.getRedeemablePoints();
        customer.setRedeemablePoints(previousPoints + pointsToRefund);
        customerRepository.save(customer);

        log.info("Customer points credited | customerId: {}, previousBalance: {}, newBalance: {}", 
                customer.getCustomerId(), previousPoints, customer.getRedeemablePoints());

        // Save successful refund
        Transaction refundTxn = Transaction.builder()
                .customerId(customer.getCustomerId())
                .transactionType(TransactionType.REFUND)
                .status(TransactionStatus.SUCCESS)
                .timestamp(LocalDateTime.now())
                .points(pointsToRefund)
                .redeemTxnId(requestDto.getRedeemTxnId())
                .refundTxnId(UUID.randomUUID().toString())
                .build();

        Transaction saved = transactionRepository.save(refundTxn);
        log.info("Refund process completed successfully | refundTxnId: {}, redeemTxnId: {}, customerId: {}, pointsRefunded: {}",
                saved.getRefundTxnId(), saved.getRedeemTxnId(), customer.getCustomerId(), pointsToRefund);

        return saved;
    }

    @Override
    public List<Transaction> getTransactionsByCustomer(Long customerId) {
        log.debug("Fetching transactions for customerId: {}", customerId);
        List<Transaction> transactions = transactionRepository.findByCustomerId(customerId);
        log.info("Found {} transactions for customerId: {}", transactions.size(), customerId);
        return transactions;
    }

    private void saveFailedRefundAudit(Long customerId, String redeemTxnId, Double points) {
        Transaction failedRefund = Transaction.builder()
                .customerId(customerId)
                .transactionType(TransactionType.REFUND)
                .status(TransactionStatus.FAILURE)
                .timestamp(LocalDateTime.now())
                .points(points)
                .redeemTxnId(redeemTxnId)
                .refundTxnId(UUID.randomUUID().toString())
                .build();
        transactionRepository.save(failedRefund);
    }

    private void validateActive(Customer customer) {
        if (customer.getAccountStatus() == AccountStatus.INACTIVE) {
            log.warn("Blocked transaction attempt — customerId: {} is INACTIVE", customer.getCustomerId());
            throw new CustomerAccountDeactivatedException(
                    "Customer account is inactive for id: " + customer.getCustomerId());
        }
    }
}