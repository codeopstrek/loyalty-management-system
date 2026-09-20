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
import tools.jackson.databind.ObjectMapper;

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
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	@Transactional
	public Transaction earnPoints(AddPointsRequestDto requestDto) {
		String payload = toJsonString(requestDto);

		log.atInfo().setMessage("Initiating EARN transaction process")
				.addKeyValue("customerId", requestDto.getCustomerId())
				.addKeyValue("points", requestDto.getPoints())
				.addKeyValue("payload", payload).log();

		Customer customer = customerService.getCustomerById(requestDto.getCustomerId());
		validateActive(customer);

		log.atInfo().setMessage("Customer verified and active").addKeyValue("customerId", customer.getCustomerId())
				.addKeyValue("currentBalance", customer.getRedeemablePoints()).log();

		double previousPoints = customer.getRedeemablePoints();
		customer.setRedeemablePoints(previousPoints + requestDto.getPoints());
		customerRepository.save(customer);

		log.atInfo().setMessage("Customer points credited").addKeyValue("customerId", customer.getCustomerId())
				.addKeyValue("previousBalance", previousPoints)
				.addKeyValue("newBalance", customer.getRedeemablePoints()).log();

		Transaction transaction = Transaction.builder().customerId(customer.getCustomerId())
				.transactionType(TransactionType.EARNED).status(TransactionStatus.SUCCESS)
				.timestamp(LocalDateTime.now()).points(requestDto.getPoints()).earnTxnId(UUID.randomUUID().toString())
				.build();

		Transaction saved = transactionRepository.save(transaction);

		log.atInfo().setMessage("EARN transaction completed successfully")
				.addKeyValue("earnTxnId", saved.getEarnTxnId()).addKeyValue("customerId", customer.getCustomerId())
				.addKeyValue("pointsEarned", saved.getPoints()).addKeyValue("payload", toJsonString(saved)).log();

		return saved;
	}

	@Override
	@Transactional
	public Transaction redeemPoints(RedeemRequestDto requestDto) {
		String payload = toJsonString(requestDto);

		log.atInfo().setMessage("Initiating REDEEM transaction process")
				.addKeyValue("customerId", requestDto.getCustomerId()).addKeyValue("points", requestDto.getPoints())
				.addKeyValue("payload", payload).log();

		Customer customer = customerService.getCustomerById(requestDto.getCustomerId());
		validateActive(customer);

		log.atInfo().setMessage("Customer verified and active").addKeyValue("customerId", customer.getCustomerId())
				.addKeyValue("currentBalance", customer.getRedeemablePoints()).log();

		if (customer.getRedeemablePoints() < requestDto.getPoints()) {
			Transaction failedTxn = transactionRepository.save(
					Transaction.builder().customerId(customer.getCustomerId()).transactionType(TransactionType.REDEEM)
							.status(TransactionStatus.FAILURE).timestamp(LocalDateTime.now())
							.points(requestDto.getPoints()).redeemTxnId(UUID.randomUUID().toString()).build());

			log.atWarn().setMessage("REDEEM transaction failed: Insufficient points")
					.addKeyValue("customerId", customer.getCustomerId())
					.addKeyValue("availablePoints", customer.getRedeemablePoints())
					.addKeyValue("requestedPoints", requestDto.getPoints())
					.addKeyValue("redeemTxnId", failedTxn.getRedeemTxnId())
					.addKeyValue("payload", toJsonString(failedTxn)).log();

			throw new PointsNotAvailableException("Insufficient points for customer id: " + customer.getCustomerId());
		}

		double previousPoints = customer.getRedeemablePoints();
		customer.setRedeemablePoints(previousPoints - requestDto.getPoints());
		customerRepository.save(customer);

		log.atInfo().setMessage("Customer points debited").addKeyValue("customerId", customer.getCustomerId())
				.addKeyValue("previousBalance", previousPoints)
				.addKeyValue("newBalance", customer.getRedeemablePoints()).log();

		Transaction transaction = Transaction.builder().customerId(customer.getCustomerId())
				.transactionType(TransactionType.REDEEM).status(TransactionStatus.SUCCESS)
				.timestamp(LocalDateTime.now()).points(requestDto.getPoints()).redeemTxnId(UUID.randomUUID().toString())
				.build();

		Transaction saved = transactionRepository.save(transaction);

		log.atInfo().setMessage("REDEEM transaction completed successfully")
				.addKeyValue("redeemTxnId", saved.getRedeemTxnId()).addKeyValue("customerId", customer.getCustomerId())
				.addKeyValue("pointsRedeemed", saved.getPoints()).addKeyValue("payload", toJsonString(saved)).log();

		return saved;
	}

	@Override
	@Transactional
	public Transaction refundPoints(RefundRequestDto requestDto) {
		String payload = toJsonString(requestDto);

		log.atInfo().setMessage("Initiating REFUND transaction process")
				.addKeyValue("customerId", requestDto.getCustomerId())
				.addKeyValue("redeemTxnId", requestDto.getRedeemTxnId()).addKeyValue("payload", payload)
				.log();

		Customer customer = customerService.getCustomerById(requestDto.getCustomerId());
		validateActive(customer);

		log.atInfo().setMessage("Customer verified and active").addKeyValue("customerId", customer.getCustomerId())
				.addKeyValue("currentBalance", customer.getRedeemablePoints()).log();

		List<Transaction> relatedTxns = transactionRepository
				.findByRedeemTxnIdAndCustomerId(requestDto.getRedeemTxnId(), requestDto.getCustomerId());

		Transaction originalRedeemTxn = relatedTxns.stream().filter(
				t -> t.getTransactionType() == TransactionType.REDEEM && t.getStatus() == TransactionStatus.SUCCESS)
				.findFirst().orElseThrow(() -> {
					log.atWarn().setMessage("Refund failed: Original redemption not found")
							.addKeyValue("customerId", requestDto.getCustomerId())
							.addKeyValue("redeemTxnId", requestDto.getRedeemTxnId())
							.addKeyValue("payload", payload).log();

					saveFailedRefundAudit(customer.getCustomerId(), requestDto.getRedeemTxnId(), 0.0);

					return new TransactionNotFoundException("Redeem transaction not found with id: "
							+ requestDto.getRedeemTxnId() + " for customer id: " + requestDto.getCustomerId());
				});

		double pointsToRefund = originalRedeemTxn.getPoints();

		log.atInfo().setMessage("Original redemption transaction verified")
				.addKeyValue("redeemTxnId", requestDto.getRedeemTxnId()).addKeyValue("pointsToRefund", pointsToRefund)
				.log();

		Optional<Transaction> existingRefund = relatedTxns.stream().filter(
				t -> t.getTransactionType() == TransactionType.REFUND && t.getStatus() == TransactionStatus.SUCCESS)
				.findFirst();

		if (existingRefund.isPresent()) {
			String existingRefundTxnId = existingRefund.get().getRefundTxnId();

			log.atWarn().setMessage("Refund failed: Duplicate refund request")
					.addKeyValue("customerId", requestDto.getCustomerId())
					.addKeyValue("redeemTxnId", requestDto.getRedeemTxnId())
					.addKeyValue("existingRefundTxnId", existingRefundTxnId)
					.addKeyValue("payload", payload).log();

			saveFailedRefundAudit(customer.getCustomerId(), requestDto.getRedeemTxnId(), pointsToRefund);

			throw new DuplicateResourceException("Refund already initiated for redeemTxnId: "
					+ requestDto.getRedeemTxnId() + " (existing refundTxnId: " + existingRefundTxnId + ")",
					"redeemTxnId");
		}

		double previousPoints = customer.getRedeemablePoints();
		customer.setRedeemablePoints(previousPoints + pointsToRefund);
		customerRepository.save(customer);

		log.atInfo().setMessage("Customer points credited").addKeyValue("customerId", customer.getCustomerId())
				.addKeyValue("previousBalance", previousPoints)
				.addKeyValue("newBalance", customer.getRedeemablePoints()).log();

		Transaction refundTxn = Transaction.builder().customerId(customer.getCustomerId())
				.transactionType(TransactionType.REFUND).status(TransactionStatus.SUCCESS)
				.timestamp(LocalDateTime.now()).points(pointsToRefund).redeemTxnId(requestDto.getRedeemTxnId())
				.refundTxnId(UUID.randomUUID().toString()).build();

		Transaction saved = transactionRepository.save(refundTxn);

		log.atInfo().setMessage("Refund process completed successfully")
				.addKeyValue("refundTxnId", saved.getRefundTxnId()).addKeyValue("redeemTxnId", saved.getRedeemTxnId())
				.addKeyValue("customerId", customer.getCustomerId()).addKeyValue("pointsRefunded", pointsToRefund)
				.addKeyValue("previousBalance", previousPoints)
				.addKeyValue("newBalance", customer.getRedeemablePoints()).addKeyValue("payload", toJsonString(saved))
				.log();

		return saved;
	}

	@Override
	public List<Transaction> getTransactionsByCustomer(Long customerId) {
		List<Transaction> transactions = transactionRepository.findByCustomerId(customerId);
		log.atInfo().setMessage("Customer transactions fetched successfully").addKeyValue("customerId", customerId)
				.addKeyValue("transactionCount", transactions.size())
				.addKeyValue("payload", toJsonString(transactions)).log();
		return transactions;
	}

	private void saveFailedRefundAudit(Long customerId, String redeemTxnId, Double points) {
		Transaction failedRefund = Transaction.builder().customerId(customerId).transactionType(TransactionType.REFUND)
				.status(TransactionStatus.FAILURE).timestamp(LocalDateTime.now()).points(points)
				.redeemTxnId(redeemTxnId).refundTxnId(UUID.randomUUID().toString()).build();

		transactionRepository.save(failedRefund);
	}

	private void validateActive(Customer customer) {
		if (customer.getAccountStatus() == AccountStatus.INACTIVE) {
			log.atWarn().setMessage("Blocked transaction attempt: Customer account is INACTIVE")
					.addKeyValue("customerId", customer.getCustomerId()).log();

			throw new CustomerAccountDeactivatedException(
					"Customer account is inactive for id: " + customer.getCustomerId());
		}
	}

	/**
	 * Helper method to convert Objects/DTOs safely to JSON string representation for SLF4J structured logging.
	 */
	private String toJsonString(Object object) {
		try {
			return objectMapper.writeValueAsString(object);
		} catch (Exception e) {
			return String.valueOf(object);
		}
	}
}