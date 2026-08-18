package com.loyalty.loyaltyprogram.exception;

public class CustomerAccountDeactivatedException extends RuntimeException {
    public CustomerAccountDeactivatedException(String message) {
        super(message);
    }
}