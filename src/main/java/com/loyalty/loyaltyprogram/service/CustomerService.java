package com.loyalty.loyaltyprogram.service;

import com.loyalty.loyaltyprogram.dto.request.CustomerRequestDto;
import com.loyalty.loyaltyprogram.model.Customer;

import java.util.List;

public interface CustomerService {
    Customer registerCustomer(CustomerRequestDto requestDto);
    Customer getCustomerById(Long customerId);
    List<Customer> getAllCustomers();
    Customer updateCustomer(Long customerId, CustomerRequestDto requestDto);
    void deleteCustomer(Long customerId);
    Customer deactivateCustomer(Long customerId);
    Customer activateCustomer(Long customerId);
}