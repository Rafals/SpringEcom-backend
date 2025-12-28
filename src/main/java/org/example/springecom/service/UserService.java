package org.example.springecom.service;

import org.example.springecom.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepo userRepo;

    public String login(String username, String password) {
        return "userRepo.login(username, password);";
    }

}
