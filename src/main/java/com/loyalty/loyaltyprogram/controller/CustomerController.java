package com.loyalty.loyaltyprogram.controller;

import com.loyalty.loyaltyprogram.dto.request.CustomerRequestDto;
import com.loyalty.loyaltyprogram.dto.response.ApiResponse;
import com.loyalty.loyaltyprogram.model.Customer;
import com.loyalty.loyaltyprogram.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

	private final CustomerService customerService;

	@PostMapping
	public ResponseEntity<ApiResponse<Customer>> registerCustomer(@Valid @RequestBody CustomerRequestDto requestDto) {
		Customer customer = customerService.registerCustomer(requestDto);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success(HttpStatus.CREATED.value(), "Customer registered successfully", customer));
	}

	@GetMapping("/{customerId}")
	public ResponseEntity<ApiResponse<Customer>> getCustomer(@PathVariable Long customerId) {
		Customer customer = customerService.getCustomerById(customerId);
		return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Customer fetched successfully", customer));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<Customer>>> getAllCustomers() {
		List<Customer> customers = customerService.getAllCustomers();
		return ResponseEntity
				.ok(ApiResponse.success(HttpStatus.OK.value(), "Customers fetched successfully", customers));
	}

	@PutMapping("/{customerId}")
	public ResponseEntity<ApiResponse<Customer>> updateCustomer(@PathVariable Long customerId,
			@Valid @RequestBody CustomerRequestDto requestDto) {
		Customer customer = customerService.updateCustomer(customerId, requestDto);
		return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Customer updated successfully", customer));
	}

	@DeleteMapping("/{customerId}")
	public ResponseEntity<ApiResponse<Void>> deleteCustomer(@PathVariable Long customerId) {
		customerService.deleteCustomer(customerId);
		return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Customer deleted successfully", null));
	}

	@PatchMapping("/{customerId}/deactivate")
	public ResponseEntity<ApiResponse<Customer>> deactivateCustomer(@PathVariable Long customerId) {
		Customer customer = customerService.deactivateCustomer(customerId);
		return ResponseEntity
				.ok(ApiResponse.success(HttpStatus.OK.value(), "Customer deactivated successfully", customer));
	}

	@PatchMapping("/{customerId}/activate")
	public ResponseEntity<ApiResponse<Customer>> activateCustomer(@PathVariable Long customerId) {
		Customer customer = customerService.activateCustomer(customerId);
		return ResponseEntity
				.ok(ApiResponse.success(HttpStatus.OK.value(), "Customer activated successfully", customer));
	}
}