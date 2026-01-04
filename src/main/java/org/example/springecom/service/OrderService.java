package org.example.springecom.service;

import org.example.springecom.model.*;
import org.example.springecom.model.dto.OrderItemResponse;
import org.example.springecom.model.dto.OrderRequest;
import org.example.springecom.model.dto.OrderResponse;
import org.example.springecom.repo.CouponRepo;
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
    @Autowired private CouponRepo couponRepo; // Wstrzykujemy repo kuponów

    @Transactional
    public OrderResponse placeOrder(OrderRequest request, String email) {
        User user = userService.getUserByEmail(email);

        // 1. Pobierz koszyk
        List<CartItem> cartItems = cartService.getCartByUser(email);
        if (cartItems.isEmpty()) throw new RuntimeException("Cart is empty");

        // 2. Oblicz sumę koszyka (Logika Biznesowa - obliczamy na backendzie!)
        BigDecimal productsTotal = BigDecimal.ZERO;
        for (CartItem item : cartItems) {
            BigDecimal itemTotal = item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            productsTotal = productsTotal.add(itemTotal);
        }

        // 3. Przygotuj zamówienie
        Order order = new Order();
        order.setOrderId("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setUser(user);
        order.setCustomerName(request.firstName() + " " + request.lastName());
        order.setEmail(email);
        order.setStatus("PAID");
        order.setOrderDate(LocalDate.now());

        // --- SEKCJA KUPONÓW START ---
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal finalTotal = productsTotal; // Na razie to sama cena produktów

        // Jeśli w requescie przyszedł kod kuponu...
        if (request.couponCode() != null && !request.couponCode().isEmpty()) {
            Coupon coupon = couponRepo.findByCode(request.couponCode())
                    .orElseThrow(() -> new RuntimeException("Invalid coupon code")); // Lub po prostu zignoruj

            if (!coupon.isActive()) {
                throw new RuntimeException("Coupon is not active");
            }

            // Oblicz zniżkę: Cena * (Procent / 100)
            BigDecimal discountPercent = BigDecimal.valueOf(coupon.getDiscountPercent());
            BigDecimal discountFactor = discountPercent.divide(BigDecimal.valueOf(100));

            discountAmount = productsTotal.multiply(discountFactor);
            finalTotal = productsTotal.subtract(discountAmount);

            // Zapisz info o kuponie w zamówieniu
            order.setCouponCode(coupon.getCode());
            order.setDiscountAmount(discountAmount);
        }

        // Dodaj koszt wysyłki (tutaj upraszczamy, że shippingMethod wpływa na cenę,
        // w idealnym świecie powinieneś mieć mapę cen wysyłki w bazie lub serwisie)
        BigDecimal shippingCost = calculateShippingCost(request.shippingMethod());
        finalTotal = finalTotal.add(shippingCost);

        order.setTotalAmount(finalTotal);
        // --- SEKCJA KUPONÓW KONIEC ---

        // 4. Przetwarzanie produktów (Stock)
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();

            // Sprawdź czy jest towar
            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new RuntimeException("Not enough stock for: " + product.getName());
            }

            // Zdejmij ze stanu
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

        // 5. Wyczyść koszyk
        cartService.clearUserCart(email);

        return convertToResponse(savedOrder);
    }

    // Prosta metoda pomocnicza do wysyłki (żeby nie śmiecić w głównej metodzie)
    private BigDecimal calculateShippingCost(String shippingMethod) {
        if (shippingMethod == null) return BigDecimal.ZERO;
        switch (shippingMethod) {
            case "DHL": return BigDecimal.valueOf(15.00);
            case "DPD": return BigDecimal.valueOf(12.00);
            case "Poczta Polska": return BigDecimal.valueOf(8.50);
            default: return BigDecimal.ZERO;
        }
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

        // Tutaj można dodać pola do OrderResponse (np. totalAmount, discount),
        // jeśli zaktualizujesz ten rekord w przyszłości.
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