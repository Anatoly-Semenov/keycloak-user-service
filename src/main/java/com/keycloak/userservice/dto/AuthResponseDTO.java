package com.keycloak.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {
    
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
    private long refreshExpiresIn;
    private String userId;
    private List<String> roles;
} 