package com.keycloak.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequestDTO {
    
    @NotBlank(message = "Refresh токен обязателен")
    private String refreshToken;
} 