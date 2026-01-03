package org.example.springecom.service;

import jakarta.transaction.Transactional;
import org.example.springecom.model.CartItem;
import org.example.springecom.model.Product;
import org.example.springecom.model.User;
import org.example.springecom.repo.CartItemRepo;
import org.example.springecom.repo.ProductRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartService {

    @Autowired
    private CartItemRepo cartRepo;

    @Autowired
    private UserService userService;

    @Autowired
    private ProductRepo productRepo;

    public List<CartItem> getCartByUser(String email) {
        User user = userService.getUserByEmail(email);
        return cartRepo.findByUser(user);
    }

    @Transactional
    public void addItemToCart(String email, Long productId, int quantity) {
        User user = userService.getUserByEmail(email);
        Product product = productRepo.findById(Math.toIntExact(productId))
                .orElseThrow(() -> new RuntimeException("Product not found"));

        cartRepo.findByUserAndProduct(user, product).ifPresentOrElse(
                item -> {
                    item.setQuantity(item.getQuantity() + quantity);
                    cartRepo.save(item);
                },
                () -> {
                    CartItem newItem = new CartItem();
                    newItem.setUser(user);
                    newItem.setProduct(product);
                    newItem.setQuantity(quantity);
                    cartRepo.save(newItem);
                }
        );
    }

    @Transactional
    public void removeItemFromCart(String email, Long productId) {
        User user = userService.getUserByEmail(email);
        Product product = productRepo.findById(Math.toIntExact(productId))
                .orElseThrow(() -> new RuntimeException("Product not found"));

        cartRepo.findByUserAndProduct(user, product)
                .ifPresent(item -> cartRepo.delete(item));
    }

    @Transactional
    public void clearUserCart(String email) {
        User user = userService.getUserByEmail(email);
        cartRepo.deleteByUser(user);
    }


}