package com.keycloak.userservice.config;

import org.apache.catalina.connector.Connector;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class GracefulShutdownConfig {

    @Bean
    public GracefulShutdown gracefulShutdown() {
        return new GracefulShutdown();
    }

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> factory.addConnectorCustomizers(gracefulShutdown());
    }

    public static class GracefulShutdown implements TomcatConnectorCustomizer, ApplicationListener<ContextClosedEvent> {

        private static final long SHUTDOWN_TIMEOUT = 30; // секунды
        private volatile Connector connector;

        @Override
        public void customize(Connector connector) {
            this.connector = connector;
        }

        @Override
        public void onApplicationEvent(ContextClosedEvent event) {
            if (this.connector == null) {
                return;
            }

            log.info("Запуск процедуры graceful shutdown...");
            this.connector.pause();

            Executor executor = this.connector.getProtocolHandler().getExecutor();
            if (executor instanceof ThreadPoolExecutor threadPoolExecutor) {
                try {
                    log.info("Ожидание завершения выполнения активных запросов...");
                    threadPoolExecutor.shutdown();
                    if (!threadPoolExecutor.awaitTermination(SHUTDOWN_TIMEOUT, TimeUnit.SECONDS)) {
                        log.warn("Некоторые запросы не завершились в течение {}с, принудительное завершение", SHUTDOWN_TIMEOUT);
                        threadPoolExecutor.shutdownNow();
                    } else {
                        log.info("Все запросы успешно завершены");
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    log.error("Прерывание во время graceful shutdown", ex);
                }
            }
            log.info("Graceful shutdown завершен успешно");
        }
    }
} 