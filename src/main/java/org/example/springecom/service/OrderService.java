package org.example.springecom.service;

import org.example.springecom.model.Order;
import org.example.springecom.model.OrderItem;
import org.example.springecom.model.Product;
import org.example.springecom.model.User;
import org.example.springecom.model.dto.OrderItemRequest;
import org.example.springecom.model.dto.OrderItemResponse;
import org.example.springecom.model.dto.OrderRequest;
import org.example.springecom.model.dto.OrderResponse;
import org.example.springecom.repo.OrderRepo;
import org.example.springecom.repo.ProductRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    @Autowired
    private ProductRepo productRepo;
    @Autowired
    private OrderRepo orderRepo;
    @Autowired
    private UserService userService;

    // Dodano parametr email, aby powiązać zamówienie z użytkownikiem
    public OrderResponse placeOrder(OrderRequest request, String email) {
        User user = userService.getUserByEmail(email);

        Order order = new Order();
        String orderId = "ORD " + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        order.setOrderId(orderId);
        order.setUser(user); // PRZYPISANIE UŻYTKOWNIKA
        order.setCustomerName(request.customerName());
        order.setEmail(request.email());
        order.setStatus("PLACED");
        order.setOrderDate(LocalDate.now());

        List<OrderItem> orderItems = new ArrayList<>();
        for(OrderItemRequest itemReq : request.items()) {
            Product product = productRepo.findById(itemReq.productId())
                    .orElseThrow(() -> new RuntimeException("Product Not Found"));

            product.setStockQuantity(product.getStockQuantity() - itemReq.quantity());
            productRepo.save(product);

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .quantity(itemReq.quantity())
                    .totalPrice(product.getPrice().multiply(BigDecimal.valueOf(itemReq.quantity())))
                    .order(order)
                    .build();
            orderItems.add(orderItem);
        }

        order.setOrderItems(orderItems);
        Order savedOrder = orderRepo.save(order);
        return convertToResponse(savedOrder);
    }

    // Nowa logika: Admin widzi wszystko, User tylko swoje
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

    // Helper, żeby nie powtarzać kodu konwersji
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