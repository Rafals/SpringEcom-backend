package org.example.springecom.controller;

import org.example.springecom.model.User;
import org.example.springecom.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class UserController {

    @Autowired
    UserService userService;

    @PostMapping("/auth/login")
    public ResponseEntity<String> login(@RequestBody User user) {
        userService.login(user.getEmail(), user.getPassword());
        System.out.println(user.getEmail() + " " + user.getPassword());
        return new ResponseEntity<>("Login successful", HttpStatus.OK);
    }

}
