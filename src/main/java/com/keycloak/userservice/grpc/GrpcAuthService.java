package com.keycloak.userservice.grpc;

import com.keycloak.userservice.dto.AuthRequestDTO;
import com.keycloak.userservice.dto.AuthResponseDTO;
import com.keycloak.userservice.dto.RefreshTokenRequestDTO;
import com.keycloak.userservice.dto.RegistrationRequestDTO;
import com.keycloak.userservice.service.AuthService;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GrpcAuthService extends AuthServiceGrpc.AuthServiceImplBase {

    private final AuthService authService;

    @Autowired
    public GrpcAuthService(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void login(LoginRequest request, StreamObserver<AuthResponse> responseObserver) {
        try {
            AuthRequestDTO authRequest = new AuthRequestDTO(request.getUsername(), request.getPassword());
            AuthResponseDTO response = authService.login(authRequest);
            
            AuthResponse grpcResponse = AuthResponse.newBuilder()
                    .setAccessToken(response.accessToken)
                    .setRefreshToken(response.refreshToken)
                    .setTokenType(response.tokenType)
                    .setExpiresIn(response.expiresIn)
                    .setRefreshExpiresIn(response.refreshExpiresIn)
                    .setUserId(response.userId)
                    .addAllRoles(response.roles)
                    .build();
            
            responseObserver.onNext(grpcResponse);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void refreshToken(RefreshTokenRequest request, StreamObserver<AuthResponse> responseObserver) {
        try {
            RefreshTokenRequestDTO refreshRequest = new RefreshTokenRequestDTO();
            refreshRequest.refreshToken = request.getRefreshToken();
            
            AuthResponseDTO response = authService.refreshToken(refreshRequest);
            
            AuthResponse grpcResponse = AuthResponse.newBuilder()
                    .setAccessToken(response.accessToken)
                    .setRefreshToken(response.refreshToken)
                    .setTokenType(response.tokenType)
                    .setExpiresIn(response.expiresIn)
                    .setRefreshExpiresIn(response.refreshExpiresIn)
                    .setUserId(response.userId)
                    .addAllRoles(response.roles)
                    .build();
            
            responseObserver.onNext(grpcResponse);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void register(RegisterRequest request, StreamObserver<AuthResponse> responseObserver) {
        try {
            RegistrationRequestDTO registerRequest = new RegistrationRequestDTO(
                    request.getUsername(),
                    request.getPassword(),
                    request.getEmail(),
                    request.getFirstName(),
                    request.getLastName(),
                    request.getPhoneNumber()
            );
            
            AuthResponseDTO response = authService.register(registerRequest);
            
            AuthResponse grpcResponse = AuthResponse.newBuilder()
                    .setAccessToken(response.accessToken)
                    .setRefreshToken(response.refreshToken)
                    .setTokenType(response.tokenType)
                    .setExpiresIn(response.expiresIn)
                    .setRefreshExpiresIn(response.refreshExpiresIn)
                    .setUserId(response.userId)
                    .addAllRoles(response.roles)
                    .build();
            
            responseObserver.onNext(grpcResponse);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }
} 