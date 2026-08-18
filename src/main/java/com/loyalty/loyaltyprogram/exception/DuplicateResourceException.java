package com.loyalty.loyaltyprogram.exception;

import lombok.Getter;

@Getter
public class DuplicateResourceException extends RuntimeException {

    private final String fieldName;
    private final String fieldValue;

    public DuplicateResourceException(String fieldName, String fieldValue) {
        super(String.format("%s already exists with value: %s", fieldName, fieldValue));
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }
}