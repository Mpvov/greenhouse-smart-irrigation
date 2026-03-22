package com.thesis.irrigation.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * WebConfig — Global configuration for Spring WebFlux.
 * Configures CORS to allow the React frontend (running on port 5173 or 80)
 * to communicate with the backend during development and deployment.
 */
@Configuration
public class WebConfig implements WebFluxConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                    "http://localhost:5173", // Vite (Host machine)
                    "http://localhost",      // Nginx / Host machine
                    "http://127.0.0.1:5173",
                    "http://127.0.0.1"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
