package com.leetai.controller;

import com.leetai.dto.UserResponse;
import com.leetai.model.User;
import com.leetai.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Returns the currently authenticated user, resolved from the JWT. */
    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        String email = authentication.getName(); // JWT subject, set by JwtAuthFilter
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        return new UserResponse(user.getEmail(), user.getName(), user.getAvatarUrl(), user.getRole().name());
    }
}
