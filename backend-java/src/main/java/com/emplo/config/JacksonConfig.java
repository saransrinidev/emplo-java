package com.emplo.config;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    /**
     * Customize the Spring-managed ObjectMapper instead of replacing it.
     * - snake_case naming to match the original Python API
     * - Java 8 date/time serialization (ISO strings, not timestamps)
     * - Hibernate6Module so lazy-loaded JPA associations serialize as null
     *   instead of throwing LazyInitializationException.
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        Hibernate6Module hibernateModule = new Hibernate6Module();
        // Do not force-load lazy associations; emit null for uninitialized proxies.
        hibernateModule.disable(Hibernate6Module.Feature.FORCE_LAZY_LOADING);

        return builder -> {
            builder.propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
            builder.modules(new JavaTimeModule(), hibernateModule);
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        };
    }
}
