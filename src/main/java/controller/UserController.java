package saimart.controller;

import saimart.entity.User;
import saimart.repository.UserRepository;

import org.springframework.web.bind.annotation.*;

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

        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return "Email already registered";
        }

        userRepository.save(user);

        return "Account created successfully";
    }

    @PostMapping("/login")
    public String login(@RequestBody User user) {

        return userRepository.findByEmail(user.getEmail())
                .filter(existingUser ->
                        existingUser.getPassword().equals(user.getPassword()))
                .map(existingUser -> "Login successful")
                .orElse("Invalid email or password");
    }
}