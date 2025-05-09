package com.keycloak.userservice.controller;

import com.keycloak.userservice.dto.AuthRequestDTO;
import com.keycloak.userservice.dto.AuthResponseDTO;
import com.keycloak.userservice.dto.RefreshTokenRequestDTO;
import com.keycloak.userservice.dto.RegistrationRequestDTO;
import com.keycloak.userservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody AuthRequestDTO request) {
        log.info("Запрос на аутентификацию пользователя: {}", request.getUsername());
        AuthResponseDTO response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDTO> refreshToken(@Valid @RequestBody RefreshTokenRequestDTO request) {
        log.info("Запрос на обновление токена");
        AuthResponseDTO response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody RegistrationRequestDTO request) {
        log.info("Запрос на регистрацию пользователя: {}", request.getUsername());
        AuthResponseDTO response = authService.register(request);
        return ResponseEntity.ok(response);
    }
} 