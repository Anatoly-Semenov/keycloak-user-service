package com.keycloak.userservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class EnvironmentValidator {

    private static final Logger log = LoggerFactory.getLogger(EnvironmentValidator.class);

    @Value("${keycloak.auth-server-url}")
    private String keycloakServerUrl;

    @Value("${keycloak.realm}")
    private String keycloakRealm;

    @Value("${keycloak.resource}")
    private String keycloakClientId;

    @Value("${keycloak.credentials.secret}")
    private String keycloakClientSecret;

    @Bean
    public CommandLineRunner validateEnvironment() {
        return args -> {
            log.info("Проверка обязательных переменных окружения");

            List<String> missingVars = new ArrayList<>();

            checkVar(keycloakServerUrl, "KEYCLOAK_AUTH_SERVER_URL", missingVars);
            checkVar(keycloakRealm, "KEYCLOAK_REALM", missingVars);
            checkVar(keycloakClientId, "KEYCLOAK_RESOURCE", missingVars);
            checkVar(keycloakClientSecret, "KEYCLOAK_CREDENTIALS_SECRET", missingVars);

            if (!missingVars.isEmpty()) {
                log.error("Отсутствуют обязательные переменные окружения: {}", String.join(", ", missingVars));
                // В production можно раскомментировать для аварийного завершения
                // System.exit(1);
            } else {
                log.info("Все обязательные переменные окружения настроены");
            }
        };
    }

    private void checkVar(String value, String name, List<String> missingVars) {
        if (value == null || value.trim().isEmpty() || value.equals("your-client-secret")) {
            missingVars.add(name);
        }
    }
} 