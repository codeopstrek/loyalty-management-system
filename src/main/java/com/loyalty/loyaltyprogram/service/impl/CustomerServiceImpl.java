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
import tools.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Customer registerCustomer(CustomerRequestDto requestDto) {
        String payload = toJsonString(requestDto);

        log.atInfo()
                .setMessage("Initiating customer registration")
                .addKeyValue("email", requestDto.getEmail())
                .addKeyValue("payload", payload)
                .log();

        if (customerRepository.findByEmail(requestDto.getEmail()).isPresent()) {
            log.atWarn()
                    .setMessage("Customer registration failed: Duplicate email")
                    .addKeyValue("email", requestDto.getEmail())
                    .addKeyValue("payload", payload)
                    .log();
            throw new DuplicateResourceException("email", requestDto.getEmail());
        }

        Customer customer = Customer.builder()
                .name(requestDto.getName())
                .email(requestDto.getEmail())
                .redeemablePoints(0.0)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        Customer saved = customerRepository.save(customer);

        log.atInfo()
                .setMessage("Customer registered successfully")
                .addKeyValue("customerId", saved.getCustomerId())
                .addKeyValue("payload", toJsonString(saved))
                .log();

        return saved;
    }

    @Override
    public Customer getCustomerById(Long customerId) {
        log.atInfo()
                .setMessage("Fetching customer details")
                .addKeyValue("customerId", customerId)
                .log();

        Customer customer = customerRepository.findById(customerId).orElseThrow(() -> {
            log.atWarn()
                    .setMessage("Customer search failed: Customer not found")
                    .addKeyValue("customerId", customerId)
                    .log();
            return new CustomerNotFoundException("Customer not found with id: " + customerId);
        });

        log.atInfo()
                .setMessage("Customer details retrieved successfully")
                .addKeyValue("customerId", customer.getCustomerId())
                .addKeyValue("payload", toJsonString(customer))
                .log();

        return customer;
    }

    @Override
    public List<Customer> getAllCustomers() {
        List<Customer> customers = customerRepository.findAll();

        log.atInfo()
                .setMessage("Fetched all customers successfully")
                .addKeyValue("count", customers.size())
                .addKeyValue("payload", toJsonString(customers))
                .log();

        return customers;
    }

    @Override
    public Customer updateCustomer(Long customerId, CustomerRequestDto requestDto) {
        String payload = toJsonString(requestDto);

        log.atInfo()
                .setMessage("Initiating customer update")
                .addKeyValue("customerId", customerId)
                .addKeyValue("payload", payload)
                .log();

        Customer customer = getCustomerById(customerId);

        if (!customer.getEmail().equalsIgnoreCase(requestDto.getEmail())
                && customerRepository.findByEmail(requestDto.getEmail()).isPresent()) {
            log.atWarn()
                    .setMessage("Customer update failed: Duplicate email")
                    .addKeyValue("customerId", customerId)
                    .addKeyValue("email", requestDto.getEmail())
                    .addKeyValue("payload", payload)
                    .log();
            throw new DuplicateResourceException("email", requestDto.getEmail());
        }

        customer.setName(requestDto.getName());
        customer.setEmail(requestDto.getEmail());
        Customer updated = customerRepository.save(customer);

        log.atInfo()
                .setMessage("Customer updated successfully")
                .addKeyValue("customerId", updated.getCustomerId())
                .addKeyValue("payload", toJsonString(updated))
                .log();

        return updated;
    }

    @Override
    public void deleteCustomer(Long customerId) {
        log.atInfo()
                .setMessage("Initiating customer deletion")
                .addKeyValue("customerId", customerId)
                .log();

        Customer customer = getCustomerById(customerId);
        customerRepository.delete(customer);

        log.atInfo()
                .setMessage("Customer deleted successfully")
                .addKeyValue("customerId", customerId)
                .log();
    }

    @Override
    public Customer deactivateCustomer(Long customerId) {
        log.atInfo()
                .setMessage("Initiating customer deactivation")
                .addKeyValue("customerId", customerId)
                .log();

        Customer customer = getCustomerById(customerId);
        customer.setAccountStatus(AccountStatus.INACTIVE);
        Customer updated = customerRepository.save(customer);

        log.atInfo()
                .setMessage("Customer deactivated successfully")
                .addKeyValue("customerId", updated.getCustomerId())
                .addKeyValue("payload", toJsonString(updated))
                .log();

        return updated;
    }

    @Override
    public Customer activateCustomer(Long customerId) {
        log.atInfo()
                .setMessage("Initiating customer activation")
                .addKeyValue("customerId", customerId)
                .log();

        Customer customer = getCustomerById(customerId);
        customer.setAccountStatus(AccountStatus.ACTIVE);
        Customer updated = customerRepository.save(customer);

        log.atInfo()
                .setMessage("Customer activated successfully")
                .addKeyValue("customerId", updated.getCustomerId())
                .addKeyValue("payload", toJsonString(updated))
                .log();

        return updated;
    }

    /**
     * Helper method to safely convert objects to JSON String representation for logging.
     */
    private String toJsonString(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            return String.valueOf(object);
        }
    }
}