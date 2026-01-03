package org.example.springecom.model.dto;

import java.math.BigDecimal;

public record OrderRequest(
        String firstName,
        String lastName,
        String street,
        String city,
        String zipCode,
        String shippingMethod,
        BigDecimal totalAmount
) {
}