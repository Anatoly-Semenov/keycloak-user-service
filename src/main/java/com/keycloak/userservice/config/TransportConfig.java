package com.keycloak.userservice.config;

import com.keycloak.userservice.grpc.GrpcAuthService;
import com.keycloak.userservice.grpc.GrpcUserService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Conditional;

@Configuration
public class TransportConfig {

    @Value("${grpc.server.port:9090}")
    private int grpcPort;

    @Value("${transport:http}")
    private String transport;

    @Autowired
    private GrpcAuthService grpcAuthService;

    @Autowired
    private GrpcUserService grpcUserService;

    @Bean
    @Conditional(GrpcEnabledCondition.class)
    public Server grpcServer() {
        return ServerBuilder.forPort(grpcPort)
                .addService(grpcAuthService)
                .addService(grpcUserService)
                .addService(ProtoReflectionService.newInstance())
                .build();
    }

    @Bean
    public GrpcServerRunner grpcServerRunner(Server grpcServer) {
        return new GrpcServerRunner(grpcServer);
    }
} 