package org.example.springecom.repo;

import org.example.springecom.model.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CouponRepo extends JpaRepository<Coupon,Integer> {
    Optional<Coupon> findByCode(String code);
}
