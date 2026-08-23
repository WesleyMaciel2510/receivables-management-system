package com.rms.config;

import com.rms.model.AppUser;
import com.rms.repository.AppUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityDataInitializer {
    @Bean
    CommandLineRunner initializeSecurityUser(SecurityProperties properties, AppUserRepository repository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            SecurityProperties.Bootstrap bootstrap = properties.bootstrap();
            if (!bootstrap.enabled()) {
                return;
            }
            if (bootstrap.username() == null || bootstrap.username().isBlank()
                    || bootstrap.password() == null || bootstrap.password().isBlank()) {
                throw new IllegalStateException("APP_SECURITY_USERNAME and APP_SECURITY_PASSWORD are required when bootstrap is enabled");
            }
            String passwordHash = passwordEncoder.encode(bootstrap.password());
            repository.findByUsername(bootstrap.username()).ifPresentOrElse(user -> {
                user.updateCredentials(passwordHash, bootstrap.roles());
                repository.save(user);
            }, () -> repository.save(new AppUser(bootstrap.username(), passwordHash, bootstrap.roles())));
        };
    }
}
