package com.loyalty.loyaltyprogram.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequestDto {

    @NotNull(message = "Customer id is required")
    private Long customerId;

    @NotBlank(message = "Redeem transaction id is required")
    private String redeemTxnId;
}