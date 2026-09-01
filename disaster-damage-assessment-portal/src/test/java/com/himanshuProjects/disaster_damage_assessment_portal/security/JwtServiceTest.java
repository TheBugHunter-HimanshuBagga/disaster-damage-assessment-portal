package com.himanshuProjects.disaster_damage_assessment_portal.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.lang.reflect.Field;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    void setUp() throws Exception {
        jwtService = new JwtService();
        setField("secretKey", SECRET);
        setField("jwtExpiration", 86400000L);
    }

    private void setField(String name, Object value) throws Exception {
        Field field = JwtService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(jwtService, value);
    }

    private UserDetails userDetails(String email) {
        return new User(email, "password", Collections.singletonList(
                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_CITIZEN")));
    }

    @Test
    @DisplayName("shouldGenerateJwtToken")
    void shouldGenerateJwtToken() {
        String token = jwtService.generateToken(userDetails("user@example.com"));
        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("shouldExtractUsernameFromToken")
    void shouldExtractUsernameFromToken() {
        String token = jwtService.generateToken(userDetails("user@example.com"));
        assertThat(jwtService.extractUsername(token)).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("shouldValidateValidToken")
    void shouldValidateValidToken() {
        UserDetails details = userDetails("user@example.com");
        String token = jwtService.generateToken(details);
        assertThat(jwtService.isTokenValid(token, details)).isTrue();
    }

    @Test
    @DisplayName("shouldRejectTokenForWrongUser")
    void shouldRejectTokenForWrongUser() {
        String token = jwtService.generateToken(userDetails("user@example.com"));
        assertThat(jwtService.isTokenValid(token, userDetails("other@example.com"))).isFalse();
    }

    @Test
    @DisplayName("shouldRejectExpiredToken")
    void shouldRejectExpiredToken() throws Exception {
        setField("jwtExpiration", -5000L);
        UserDetails details = userDetails("user@example.com");
        String token = jwtService.generateToken(details);
        // A token generated with a negative expiration is immediately expired.
        // Validation must not accept it as a valid token: it either fails the
        // username check or surfaces an expiration error.
        assertThatThrownBy(() -> jwtService.isTokenValid(token, details))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("shouldThrowOnInvalidTokenSignature")
    void shouldThrowOnInvalidTokenSignature() {
        String token = "invalid.token.value";
        assertThatThrownBy(() -> jwtService.extractUsername(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("shouldThrowOnGarbageToken")
    void shouldThrowOnGarbageToken() {
        assertThatThrownBy(() -> jwtService.extractUsername("not-a-token"))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("shouldGenerateDistinctTokensForDifferentUsers")
    void shouldGenerateDistinctTokensForDifferentUsers() {
        String tokenA = jwtService.generateToken(userDetails("a@example.com"));
        String tokenB = jwtService.generateToken(userDetails("b@example.com"));
        assertThat(tokenA).isNotEqualTo(tokenB);
    }
}
