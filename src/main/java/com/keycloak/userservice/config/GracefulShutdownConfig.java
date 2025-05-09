package com.keycloak.userservice.config;

import org.apache.catalina.connector.Connector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Configuration
public class GracefulShutdownConfig {

    private static final Logger log = LoggerFactory.getLogger(GracefulShutdownConfig.class);
    private static final long SHUTDOWN_TIMEOUT = 30;

    @Bean
    public GracefulShutdown gracefulShutdown() {
        return new GracefulShutdown();
    }

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> factory.addConnectorCustomizers(gracefulShutdown());
    }

    public static class GracefulShutdown implements TomcatConnectorCustomizer, ApplicationListener<ContextClosedEvent> {
        private static final Logger log = LoggerFactory.getLogger(GracefulShutdown.class);
        
        private volatile Connector connector;

        @Override
        public void customize(Connector connector) {
            this.connector = connector;
            log.info("Tomcat connector initialized for graceful shutdown");
        }

        @Override
        public void onApplicationEvent(ContextClosedEvent event) {
            log.info("Starting graceful shutdown of Tomcat with timeout {} seconds", SHUTDOWN_TIMEOUT);
            
            try {
                if (this.connector != null) {
                    this.connector.pause();
                    log.info("Paused connector successfully");
                    
                    Executor executor = this.connector.getProtocolHandler().getExecutor();
                    if (executor instanceof ThreadPoolExecutor) {
                        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executor;
                        log.info("Active threads: {}", threadPoolExecutor.getActiveCount());
                        
                        threadPoolExecutor.shutdown();
                        if (!threadPoolExecutor.awaitTermination(SHUTDOWN_TIMEOUT, TimeUnit.SECONDS)) {
                            log.warn("Tomcat thread pool did not shut down gracefully within {} seconds. Proceeding with forced shutdown", SHUTDOWN_TIMEOUT);
                            threadPoolExecutor.shutdownNow();
                        } else {
                            log.info("Tomcat thread pool shut down gracefully");
                        }
                    }
                } else {
                    log.warn("No Tomcat connector found for graceful shutdown");
                }
            } catch (Exception ex) {
                log.error("Error during graceful shutdown of Tomcat", ex);
            }
        }
    }
} 