package org.example.springecom.service;

import org.example.springecom.model.*;
import org.example.springecom.model.dto.OrderItemRequest;
import org.example.springecom.model.dto.OrderItemResponse;
import org.example.springecom.model.dto.OrderRequest;
import org.example.springecom.model.dto.OrderResponse;
import org.example.springecom.repo.OrderRepo;
import org.example.springecom.repo.ProductRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    @Autowired private ProductRepo productRepo;
    @Autowired private OrderRepo orderRepo;
    @Autowired private UserService userService;
    @Autowired private CartService cartService;

    @Transactional
    public OrderResponse placeOrder(OrderRequest request, String email) {
        User user = userService.getUserByEmail(email);

        List<CartItem> cartItems = cartService.getCartByUser(email);
        if (cartItems.isEmpty()) throw new RuntimeException("Cart is empty");

        Order order = new Order();
        order.setOrderId("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setUser(user);
        order.setCustomerName(request.firstName() + " " + request.lastName());
        order.setEmail(email);
        order.setStatus("PAID");
        order.setOrderDate(LocalDate.now());

        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();

            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new RuntimeException("Not enough stock for: " + product.getName());
            }
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepo.save(product);

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .quantity(cartItem.getQuantity())
                    .totalPrice(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())))
                    .order(order)
                    .build();
            orderItems.add(orderItem);
        }

        order.setOrderItems(orderItems);
        Order savedOrder = orderRepo.save(order);

        cartService.clearUserCart(email);

        return convertToResponse(savedOrder);
    }

    public List<OrderResponse> getAllOrderResponses(String email) {
        User user = userService.getUserByEmail(email);
        List<Order> orders;

        if ("ROLE_ADMIN".equals(user.getRole())) {
            orders = orderRepo.findAll();
        } else {
            orders = orderRepo.findByUser(user);
        }

        List<OrderResponse> orderResponses = new ArrayList<>();
        for(Order order : orders) {
            orderResponses.add(convertToResponse(order));
        }
        return orderResponses;
    }

    private OrderResponse convertToResponse(Order order) {
        List<OrderItemResponse> itemResponses = new ArrayList<>();
        for(OrderItem item : order.getOrderItems()) {
            itemResponses.add(new OrderItemResponse(
                    item.getProduct().getName(),
                    item.getQuantity(),
                    item.getTotalPrice()
            ));
        }
        return new OrderResponse(
                order.getOrderId(),
                order.getCustomerName(),
                order.getEmail(),
                order.getStatus(),
                order.getOrderDate(),
                itemResponses
        );
    }
}