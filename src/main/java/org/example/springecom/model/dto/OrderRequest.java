package org.example.springecom.model.dto;

public record OrderRequest(
    String customerName,
    String email,
    java.util.List<OrderItemRequest> items
) {
}
