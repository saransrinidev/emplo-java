package com.emplo.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    /**
     * Cache definitions:
     * - departments: rarely changes, cache 10 min
     * - designations: rarely changes, cache 10 min
     * - leave_types: rarely changes, cache 30 min
     * - holidays: rarely changes, cache 30 min
     * - onboarding_templates: rarely changes, cache 15 min
     * - dashboard_hr: expensive queries, cache 2 min
     * - dashboard_employee: per-user, cache 1 min
     * - employee_list: list queries, cache 30 sec
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats());

        // Register specific caches with different TTLs via registerCustomCache
        manager.registerCustomCache("departments",
                Caffeine.newBuilder().maximumSize(50).expireAfterWrite(10, TimeUnit.MINUTES).build());
        manager.registerCustomCache("designations",
                Caffeine.newBuilder().maximumSize(100).expireAfterWrite(10, TimeUnit.MINUTES).build());
        manager.registerCustomCache("leave_types",
                Caffeine.newBuilder().maximumSize(20).expireAfterWrite(30, TimeUnit.MINUTES).build());
        manager.registerCustomCache("holidays",
                Caffeine.newBuilder().maximumSize(100).expireAfterWrite(30, TimeUnit.MINUTES).build());
        manager.registerCustomCache("onboarding_templates",
                Caffeine.newBuilder().maximumSize(50).expireAfterWrite(15, TimeUnit.MINUTES).build());
        manager.registerCustomCache("dashboard_hr",
                Caffeine.newBuilder().maximumSize(5).expireAfterWrite(2, TimeUnit.MINUTES).build());
        manager.registerCustomCache("dashboard_employee",
                Caffeine.newBuilder().maximumSize(200).expireAfterWrite(1, TimeUnit.MINUTES).build());
        manager.registerCustomCache("employee_list",
                Caffeine.newBuilder().maximumSize(20).expireAfterWrite(30, TimeUnit.SECONDS).build());

        return manager;
    }
}
