package com.keycloak.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class RegistrationRequestDTO {
    
    @NotBlank(message = "Имя пользователя обязательно")
    public String username;
    
    @NotBlank(message = "Пароль обязателен")
    public String password;
    
    @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный формат email")
    public String email;
    
    @NotBlank(message = "Имя обязательно")
    public String firstName;
    
    @NotBlank(message = "Фамилия обязательна")
    public String lastName;
    
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Некорректный формат телефона")
    public String phoneNumber;
    
    public RegistrationRequestDTO() {
    }
    
    public RegistrationRequestDTO(String username, String password, String email, 
                                 String firstName, String lastName, String phoneNumber) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
    }
} 