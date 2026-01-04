package org.example.springecom.controller;


import org.example.springecom.model.Coupon;
import org.example.springecom.service.CouponService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    @Autowired
    private CouponService couponService;

    @GetMapping("/validate/{code}")
    public ResponseEntity<?> validateCoupon(@PathVariable String code) {
        try {
            Coupon coupon = couponService.validateCoupon(code);
            return ResponseEntity.ok(coupon);
        } catch (RuntimeException e) {
            // Zwracamy 400 Bad Request z wiadomością błędu
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')") // Tylko admin może tworzyć
    public ResponseEntity<?> createCoupon(@RequestBody Coupon coupon) {
        try {
            return ResponseEntity.ok(couponService.createCoupon(coupon));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Coupon>> getAllCoupons() {
        return ResponseEntity.ok(couponService.getAllCoupons());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCoupon(@PathVariable Long id) {
        couponService.deleteCoupon(id);
        return ResponseEntity.ok("Coupon deleted");
    }
}
