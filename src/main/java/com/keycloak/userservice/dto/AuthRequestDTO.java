package com.keycloak.userservice.dto;

import jakarta.validation.constraints.NotBlank;

public class AuthRequestDTO {
    
    @NotBlank(message = "Имя пользователя обязательно")
    public String username;
    
    @NotBlank(message = "Пароль обязателен")
    public String password;
    
    public AuthRequestDTO() {
    }
    
    public AuthRequestDTO(String username, String password) {
        this.username = username;
        this.password = password;
    }
} 