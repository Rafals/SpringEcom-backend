package org.example.springecom.model.dto;

public record OrderResponse(
    String orderId,
    String customerName,
    String email,
    String status,
    java.util.List<OrderItemResponse> items
) {
}
