package com.rms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(Jwt jwt, Bootstrap bootstrap, Cors cors) {
    public record Jwt(String issuer, String secret, long accessTokenMinutes) {}
    public record Bootstrap(boolean enabled, String username, String password, String roles) {}
    public record Cors(String allowedOrigins) {}
}
