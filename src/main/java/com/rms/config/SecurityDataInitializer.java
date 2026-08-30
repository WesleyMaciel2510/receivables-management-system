package com.rms.config;

import com.rms.model.User;
import com.rms.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("!dev")
public class SecurityDataInitializer {
    @Bean
    CommandLineRunner initializeSecurityUser(SecurityProperties properties, UserRepository repository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            SecurityProperties.Bootstrap bootstrap = properties.bootstrap();
            if (!bootstrap.enabled()) return;
            if (bootstrap.username() == null || bootstrap.username().isBlank()
                    || bootstrap.password() == null || bootstrap.password().isBlank()) {
                throw new IllegalStateException("APP_SECURITY_USERNAME and APP_SECURITY_PASSWORD are required when bootstrap is enabled");
            }
            String passwordHash = passwordEncoder.encode(bootstrap.password());
            repository.findByUsername(bootstrap.username()).ifPresentOrElse(user -> {
                user.setPassword(passwordHash);
                repository.save(user);
            }, () -> {
                User newUser = new User(bootstrap.username(), passwordHash, bootstrap.username() + "@rms.local", "Admin", "User", null);
                repository.save(newUser);
            });
        };
    }
}