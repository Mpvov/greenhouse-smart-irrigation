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

@RestController
@RequestMapping("/api/v1/auth")
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
        return userRepository.findByEmail(request.email())
                .flatMap(existingUser -> Mono.just(ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(BaseResponse.<AuthResponse>error(HttpStatus.CONFLICT.value(), "Email already exists"))))
                .switchIfEmpty(Mono.defer(() -> {
                    User newUser = User.builder()
                            .email(request.email())
                            .passwordHash(passwordEncoder.encode(request.password()))
                            .role("OWNER")
                            .build();
                    return userRepository.save(newUser)
                            .map(savedUser -> {
                                String token = jwtUtil.generateToken(savedUser.id(), savedUser.email());
                                return ResponseEntity.ok(BaseResponse.success("Success", new AuthResponse(token)));
                            });
                }));
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<BaseResponse<AuthResponse>>> login(@RequestBody LoginRequest request) {
        return userRepository.findByEmail(request.email())
                .filter(user -> passwordEncoder.matches(request.password(), user.passwordHash()))
                .map(user -> {
                    String token = jwtUtil.generateToken(user.id(), user.email());
                    return ResponseEntity.ok(BaseResponse.success("Success", new AuthResponse(token)));
                })
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(BaseResponse.<AuthResponse>error(HttpStatus.UNAUTHORIZED.value(), "Invalid credentials"))));
    }
}
