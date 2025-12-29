package org.example.springecom.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.example.springecom.model.CartItem;
import org.example.springecom.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@CrossOrigin
public class CartController {

    @Autowired
    private CartService cartService;

    // Pobierz koszyk zalogowanego użytkownika
    @GetMapping
    public ResponseEntity<List<CartItem>> getCart(Authentication authentication) {
        String email = authentication.getName(); // Wyciąga email z tokenu JWT
        return ResponseEntity.ok(cartService.getCartByUser(email));
    }

    // Dodaj produkt do koszyka (lub zwiększ ilość, jeśli już jest)
    @PostMapping("/add/{productId}")
    public ResponseEntity<?> addToCart(@PathVariable Long productId,
                                       @RequestParam(defaultValue = "1") int quantity,
                                       Authentication authentication) {
        System.out.println("DEBUG: Request reached CartController for user: " +
                (authentication != null ? authentication.getName() : "NULL"));
        String email = authentication.getName();
        cartService.addItemToCart(email, productId, quantity);
        return ResponseEntity.ok().build();
    }

    // Usuń konkretny produkt z koszyka
    @DeleteMapping("/remove/{productId}")
    public ResponseEntity<?> removeFromCart(@PathVariable Long productId, Authentication authentication) {
        String email = authentication.getName();
        cartService.removeItemFromCart(email, productId);
        return ResponseEntity.ok().build();
    }

    // Wyczyść cały koszyk (np. po złożeniu zamówienia)
    @DeleteMapping("/clear")
    public ResponseEntity<?> clearCart(Authentication authentication) {
        String email = authentication.getName();
        cartService.clearUserCart(email);
        return ResponseEntity.ok().build();
    }
}
