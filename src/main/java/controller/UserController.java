package saimart.controller;

import saimart.entity.User;
import saimart.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/signup")
    public String signup(@RequestBody User user) {
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            return "Email is required";
        }
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            return "Password is required";
        }

        String email = user.getEmail().trim().toLowerCase();
        if (userRepository.findByEmail(email).isPresent()) {
            return "Email already registered";
        }

        user.setEmail(email);
        if (user.getName() == null || user.getName().trim().isEmpty()) {
            user.setName("Saimart User");
        } else {
            user.setName(user.getName().trim());
        }

        if (user.getRole() == null ||
                (!user.getRole().equalsIgnoreCase("BUYER") &&
                 !user.getRole().equalsIgnoreCase("SELLER"))) {
            user.setRole("BUYER");
        } else {
            user.setRole(user.getRole().toUpperCase().trim());
        }

        userRepository.save(user);
        return "Account created successfully";
    }

    @PostMapping("/login")
    public ResponseEntity<User> login(@RequestBody User user) {
        if (user == null || user.getEmail() == null || user.getPassword() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        String email = user.getEmail().trim().toLowerCase();
        Optional<User> existingUserOpt = userRepository.findByEmail(email);

        boolean isDemoAccount = ("seller@saimart.com".equalsIgnoreCase(email) || "buyer@saimart.com".equalsIgnoreCase(email))
                && "password123".equals(user.getPassword());

        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            if (isDemoAccount) {
                // Ensure demo account password and role are synchronized
                existingUser.setPassword("password123");
                if (existingUser.getRole() == null || existingUser.getRole().trim().isEmpty()) {
                    existingUser.setRole("seller@saimart.com".equalsIgnoreCase(email) ? "SELLER" : "BUYER");
                }
                userRepository.save(existingUser);
                return ResponseEntity.ok(existingUser);
            }
            if (existingUser.getPassword() != null && existingUser.getPassword().equals(user.getPassword())) {
                return ResponseEntity.ok(existingUser);
            }
        } else if (isDemoAccount) {
            // Auto-create demo user if not present
            User demoUser = new User();
            demoUser.setEmail(email);
            demoUser.setPassword("password123");
            demoUser.setName("seller@saimart.com".equalsIgnoreCase(email) ? "Saimart Merchant" : "Saimart Buyer");
            demoUser.setRole("seller@saimart.com".equalsIgnoreCase(email) ? "SELLER" : "BUYER");
            User saved = userRepository.save(demoUser);
            return ResponseEntity.ok(saved);
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
    }
}