package org.example.springecom.model.dto;

public record OrderResponse(
    String orderId,
    String customerName,
    String email,
    String status,
    java.time.LocalDate orderDate,
    java.util.List<OrderItemResponse> items
) {
}
