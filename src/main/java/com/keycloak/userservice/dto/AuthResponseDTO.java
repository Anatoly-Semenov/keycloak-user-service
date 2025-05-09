package com.keycloak.userservice.dto;

import java.util.List;

public class AuthResponseDTO {
    
    public String accessToken;
    public String refreshToken;
    public String tokenType;
    public Long expiresIn;
    public Long refreshExpiresIn;
    public String userId;
    public List<String> roles;
    
    public AuthResponseDTO() {
    }
    
    public AuthResponseDTO(String accessToken, String refreshToken, String tokenType, 
                          Long expiresIn, Long refreshExpiresIn, String userId, List<String> roles) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.refreshExpiresIn = refreshExpiresIn;
        this.userId = userId;
        this.roles = roles;
    }
} 