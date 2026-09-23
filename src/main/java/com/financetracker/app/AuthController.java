package com.financetracker.app;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final UserDAO userDAO;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserDAO userDAO, PasswordEncoder passwordEncoder) {
        this.userDAO = userDAO;
        this.passwordEncoder = passwordEncoder;
    }

    public record RegisterRequest(String username, String password) {}
    public record MessageResponse(String message) {}
    public record WhoAmIResponse(String username) {}

    @PostMapping("/register")
    public MessageResponse register(@RequestBody RegisterRequest request) {
        if (request.username() == null || request.username().isBlank() ||
            request.password() == null || request.password().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username and password are required.");
        }

        if (userDAO.findByUsername(request.username()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken.");
        }

        String hashedPassword = passwordEncoder.encode(request.password());
        userDAO.insertUser(request.username(), hashedPassword);

        return new MessageResponse("User registered successfully.");
    }

    @GetMapping("/whoami")
    public WhoAmIResponse whoAmI(Authentication authentication) {
        return new WhoAmIResponse(authentication.getName());
    }
}