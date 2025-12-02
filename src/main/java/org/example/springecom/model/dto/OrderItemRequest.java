package org.example.springecom.model.dto;

public record OrderItemRequest(
    int productId,
    int quantity
) {
}
