package com.thesis.irrigation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Smart Irrigation Backend.
 *
 * @SpringBootApplication enables:
 *   - @Configuration        : Marks as configuration source
 *   - @EnableAutoConfiguration : Auto-configures Spring context (WebFlux, MongoDB, Redis...)
 *   - @ComponentScan        : Scans all sub-packages for beans (@Component, @Service, etc.)
 */
@SpringBootApplication
public class IrrigationApplication {

    public static void main(String[] args) {
        SpringApplication.run(IrrigationApplication.class, args);
    }
}
