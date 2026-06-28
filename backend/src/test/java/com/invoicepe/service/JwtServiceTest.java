package com.invoicepe.service;

import com.invoicepe.config.UserPrincipal;
import com.invoicepe.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private JwtService jwtService;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret",
                "test-secret-that-is-at-least-32-chars-long!!");
        ReflectionTestUtils.setField(jwtService, "expirationMs", 86400000L);

        User user = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .passwordHash("hash")
                .fullName("Test User")
                .plan("free")
                .build();
        principal = new UserPrincipal(user);
    }

    @Test
    void generatesValidToken() {
        String token = jwtService.generateToken(principal);
        assertNotNull(token);
        assertFalse(token.isBlank());
        assertEquals("test@example.com", jwtService.extractUsername(token));
        assertEquals(principal.getUser().getId(), jwtService.extractUserId(token));
    }

    @Test
    void validatesGoodToken() {
        String token = jwtService.generateToken(principal);
        assertTrue(jwtService.isTokenValid(token, principal));
    }

    @Test
    void rejectsExpiredToken() {
        ReflectionTestUtils.setField(jwtService, "expirationMs", -1000L);
        String token = jwtService.generateToken(principal);
        assertFalse(jwtService.isTokenValid(token, principal));
    }

    @Test
    void rejectsTamperedToken() {
        String token = jwtService.generateToken(principal);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertFalse(jwtService.isTokenValid(tampered, principal));
    }
}
