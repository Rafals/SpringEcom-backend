package org.example.springecom.controller;

import org.example.springecom.model.dto.OrderRequest;
import org.example.springecom.model.dto.OrderResponse;
import org.example.springecom.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(@RequestBody OrderRequest request, Authentication authentication) {
        // authentication.getName() zwraca email zakodowany w Twoim tokenie
        return ResponseEntity.ok(orderService.placeOrder(request, authentication.getName()));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrders(Authentication authentication) {
        return ResponseEntity.ok(orderService.getAllOrderResponses(authentication.getName()));
    }
}
