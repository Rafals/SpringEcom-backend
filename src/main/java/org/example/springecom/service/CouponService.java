package org.example.springecom.service;

import org.example.springecom.model.Coupon;
import org.example.springecom.repo.CouponRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CouponService {

    @Autowired
    private CouponRepo couponRepo;

    public Coupon validateCoupon(String code) {
        Coupon coupon = couponRepo.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Invalid coupon code"));

        if (!coupon.isActive()) {
            throw new RuntimeException("Coupon is expired or inactive");
        }

        return coupon;
    }

    public Coupon createCoupon(Coupon coupon) {
        // Tu można dodać walidację, np. czy kod nie jest pusty
        if (couponRepo.findByCode(coupon.getCode()).isPresent()) {
            throw new RuntimeException("Coupon code already exists");
        }
        return couponRepo.save(coupon);
    }

    public List<Coupon> getAllCoupons() {
        return couponRepo.findAll();
    }

    public void deleteCoupon(Long id) {
        couponRepo.deleteById(Math.toIntExact(id));
    }
}
