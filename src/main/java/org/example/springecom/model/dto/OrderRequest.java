package org.example.springecom.model.dto;

public record OrderRequest(
    String name,
    String email,
    java.util.List<OrderItemRequest> items
) {
}
