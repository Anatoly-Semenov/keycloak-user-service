package com.keycloak.userservice.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.env.Environment;

public class GrpcEnabledCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment env = context.getEnvironment();
        String transport = env.getProperty("transport", "http");
        return "grpc".equalsIgnoreCase(transport) || "both".equalsIgnoreCase(transport);
    }
} 