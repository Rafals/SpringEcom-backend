package org.example.springecom.model.dto;

public record OrderItemResponse(
    String productName,
    int quantity,
    double totalPrice
) {
}
