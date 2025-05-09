package com.keycloak.userservice.dto;

import jakarta.validation.constraints.NotBlank;

public class RefreshTokenRequestDTO {
    
    @NotBlank(message = "Refresh токен обязателен")
    public String refreshToken;
    
    public RefreshTokenRequestDTO() {
    }
    
    public RefreshTokenRequestDTO(String refreshToken) {
        this.refreshToken = refreshToken;
    }
} 