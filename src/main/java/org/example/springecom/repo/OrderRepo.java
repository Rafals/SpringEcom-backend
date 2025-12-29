package org.example.springecom.repo;

import org.example.springecom.model.Order;
import org.example.springecom.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepo extends JpaRepository<Order, Long> { // Zmienione z Integer na Long
    Optional<Order> findByOrderId(String orderId);
    List<Order> findByUser(User user);
}