package com.emplo.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Cors cors = new Cors();
    private RateLimit rateLimit = new RateLimit();

    @Getter
    @Setter
    public static class Jwt {
        private String secret = "change-me";
        private String algorithm = "HS256";
        private int accessTokenExpireMinutes = 30;
        private int refreshTokenExpireDays = 7;
    }

    @Getter
    @Setter
    public static class Cors {
        private String allowedOrigins = "http://localhost:5173,http://127.0.0.1:5173";
    }

    @Getter
    @Setter
    public static class RateLimit {
        private int defaultRequestsPerMinute = 200;
        private int loginRequestsPerMinute = 5;
        private int refreshRequestsPerMinute = 10;
    }
}
