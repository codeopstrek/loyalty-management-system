package com.loyalty.loyaltyprogram.service.impl;

import com.loyalty.loyaltyprogram.dto.request.CustomerRequestDto;
import com.loyalty.loyaltyprogram.enums.AccountStatus;
import com.loyalty.loyaltyprogram.exception.CustomerNotFoundException;
import com.loyalty.loyaltyprogram.exception.DuplicateResourceException;
import com.loyalty.loyaltyprogram.model.Customer;
import com.loyalty.loyaltyprogram.repository.CustomerRepository;
import com.loyalty.loyaltyprogram.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

	private final CustomerRepository customerRepository;

	@Override
	public Customer registerCustomer(CustomerRequestDto requestDto) {
		log.info("Registering new customer with email: {}", requestDto.getEmail());

		if (customerRepository.findByEmail(requestDto.getEmail()).isPresent()) {
			log.warn("Registration failed - duplicate email: {}", requestDto.getEmail());
			throw new DuplicateResourceException("email", requestDto.getEmail());
		}

		Customer customer = Customer.builder().name(requestDto.getName()).email(requestDto.getEmail())
				.redeemablePoints(0.0).accountStatus(AccountStatus.ACTIVE).build();
		Customer saved = customerRepository.save(customer);
		log.info("Customer registered successfully with id: {}", saved.getCustomerId());
		return saved;
	}

	@Override
	public Customer getCustomerById(Long customerId) {
		log.debug("Fetching customer with id: {}", customerId);
		return customerRepository.findById(customerId).orElseThrow(() -> {
			log.warn("Customer not found with id: {}", customerId);
			return new CustomerNotFoundException("Customer not found with id: " + customerId);
		});
	}

	@Override
	public List<Customer> getAllCustomers() {
		log.debug("Fetching all customers");
		List<Customer> customers = customerRepository.findAll();
		log.info("Fetched {} customers", customers.size());
		return customers;
	}

	@Override
	public Customer updateCustomer(Long customerId, CustomerRequestDto requestDto) {
		log.info("Updating customer with id: {}", customerId);
		Customer customer = getCustomerById(customerId);

		if (!customer.getEmail().equalsIgnoreCase(requestDto.getEmail())
				&& customerRepository.findByEmail(requestDto.getEmail()).isPresent()) {
			log.warn("Update failed - duplicate email: {}", requestDto.getEmail());
			throw new DuplicateResourceException("email", requestDto.getEmail());
		}

		customer.setName(requestDto.getName());
		customer.setEmail(requestDto.getEmail());
		Customer updated = customerRepository.save(customer);
		log.info("Customer updated successfully with id: {}", updated.getCustomerId());
		return updated;
	}

	@Override
	public void deleteCustomer(Long customerId) {
		log.info("Deleting customer with id: {}", customerId);
		Customer customer = getCustomerById(customerId);
		customerRepository.delete(customer);
		log.info("Customer deleted successfully with id: {}", customerId);
	}

	@Override
	public Customer deactivateCustomer(Long customerId) {
		log.info("Deactivating customer with id: {}", customerId);
		Customer customer = getCustomerById(customerId);
		customer.setAccountStatus(AccountStatus.INACTIVE);
		Customer updated = customerRepository.save(customer);
		log.info("Customer deactivated successfully with id: {}", updated.getCustomerId());
		return updated;
	}

	@Override
	public Customer activateCustomer(Long customerId) {
		log.info("Activating customer with id: {}", customerId);
		Customer customer = getCustomerById(customerId);
		customer.setAccountStatus(AccountStatus.ACTIVE);
		Customer updated = customerRepository.save(customer);
		log.info("Customer activated successfully with id: {}", updated.getCustomerId());
		return updated;
	}
}