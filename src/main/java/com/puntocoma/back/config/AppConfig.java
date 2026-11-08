package com.puntocoma.back.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AppConfig {
    /** Reloj inyectable para poder probar fechas de préstamo y multas. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
