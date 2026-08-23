package com.rms;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("local")
@Tag("integration")
@TestPropertySource(properties = {
        "app.security.jwt.issuer=test-issuer",
        "app.security.jwt.secret=test-secret-with-at-least-32-characters-1234567890",
        "app.security.jwt.access-token-minutes=30",
        "app.security.bootstrap.enabled=false",
        "app.encryption.enabled=true",
        "app.encryption.secret=test-encryption-secret-for-tests-32-bytes",
        "app.encryption.current-key-version=1"
})
class RmsApplicationTests {

    @Test
    void contextLoads() {
    }
}
