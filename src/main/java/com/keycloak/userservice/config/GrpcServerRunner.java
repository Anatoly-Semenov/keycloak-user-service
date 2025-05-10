package com.keycloak.userservice.config;

import io.grpc.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class GrpcServerRunner implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(GrpcServerRunner.class);

    private final Server grpcServer;

    @Autowired
    public GrpcServerRunner(Server grpcServer) {
        this.grpcServer = grpcServer;
    }

    @Override
    public void run(String... args) throws Exception {
        if (grpcServer != null) {
            grpcServer.start();
            logger.info("gRPC Server started, listening on port {}", grpcServer.getPort());
            
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down gRPC server...");
                if (grpcServer != null) {
                    grpcServer.shutdown();
                }
                logger.info("gRPC server shut down completed.");
            }));
        }
    }
} 