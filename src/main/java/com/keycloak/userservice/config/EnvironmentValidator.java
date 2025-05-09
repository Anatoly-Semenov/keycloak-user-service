package com.keycloak.userservice.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@Slf4j
public class EnvironmentValidator {

    private final Environment environment;

    private static final List<String> REQUIRED_ENV_VARS = Arrays.asList(
            "spring.datasource.url",
            "spring.datasource.username",
            "spring.datasource.password",
            "keycloak.auth-server-url",
            "keycloak.realm",
            "keycloak.resource",
            "keycloak.credentials.secret"
    );

    @Autowired
    public EnvironmentValidator(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void validateEnvironment() {
        List<String> missingEnvVars = REQUIRED_ENV_VARS.stream()
                .filter(envVar -> environment.getProperty(envVar) == null)
                .collect(Collectors.toList());

        if (!missingEnvVars.isEmpty()) {
            String errorMessage = "Отсутствуют обязательные переменные окружения: " + 
                    String.join(", ", missingEnvVars);
            log.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        log.info("Все обязательные переменные окружения успешно валидированы");
    }
} 