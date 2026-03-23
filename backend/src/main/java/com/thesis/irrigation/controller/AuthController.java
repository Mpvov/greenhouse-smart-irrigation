package com.thesis.irrigation.controller;

import com.thesis.irrigation.common.BaseResponse;
import com.thesis.irrigation.domain.dto.AuthResponse;
import com.thesis.irrigation.domain.dto.LoginRequest;
import com.thesis.irrigation.domain.model.User;
import com.thesis.irrigation.domain.repository.UserRepository;
import com.thesis.irrigation.utils.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
public class AuthController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @PostMapping("/register")
    public Mono<ResponseEntity<BaseResponse<AuthResponse>>> register(@RequestBody LoginRequest request) {
        String email = request.email();
        if (email == null || !email.contains("@")) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(BaseResponse.error(HttpStatus.BAD_REQUEST.value(), "Invalid email format")));
        }

        String suggestedId = email.split("@")[0];
        log.info("[AuthController] Attempting to register user: {} (ID: {})", email, suggestedId);

        return userRepository.findById(suggestedId)
                .flatMap(foundById -> {
                    log.warn("[AuthController] Registration failed: ID collision for {}", suggestedId);
                    return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(BaseResponse.<AuthResponse>error(HttpStatus.CONFLICT.value(), "User ID already exists")));
                })
                .switchIfEmpty(userRepository.findByEmail(email)
                        .flatMap(foundByEmail -> {
                            log.warn("[AuthController] Registration failed: Email collision for {}", email);
                            return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT)
                                    .body(BaseResponse.<AuthResponse>error(HttpStatus.CONFLICT.value(), "Email already exists")));
                        }))
                .switchIfEmpty(Mono.defer(() -> {
                    User newUser = User.builder()
                            .userId(suggestedId)
                            .email(email)
                            .passwordHash(passwordEncoder.encode(request.password()))
                            .role("OWNER")
                            .build();

                    return userRepository.save(newUser)
                            .doOnNext(saved -> log.info("[AuthController] ✅ User successfully saved to MongoDB: {}", saved.userId()))
                            .doOnError(err -> log.error("[AuthController] 🛑 Failed to save user: {}", err.getMessage()))
                            .map(savedUser -> {
                                String token = jwtUtil.generateToken(savedUser.userId(), savedUser.email());
                                return ResponseEntity.ok(BaseResponse.success("Registration successful", new AuthResponse(token)));
                            });
                }));
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<BaseResponse<AuthResponse>>> login(@RequestBody LoginRequest request) {
        return userRepository.findByEmail(request.email())
                .filter(user -> passwordEncoder.matches(request.password(), user.passwordHash()))
                .map(user -> {
                    String token = jwtUtil.generateToken(user.userId(), user.email());
                    return ResponseEntity.ok(BaseResponse.success("Success", new AuthResponse(token)));
                })
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(BaseResponse.<AuthResponse>error(HttpStatus.UNAUTHORIZED.value(), "Invalid credentials"))));
    }
}
