package com.springboot.manhaji.config;

import com.springboot.manhaji.entity.Student;
import com.springboot.manhaji.entity.User;
import com.springboot.manhaji.entity.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link JwtService}, covering the audit S2 regression: the signing
 * key must come from a single {@code Decoders.BASE64.decode(secretKey)} call against
 * a properly Base64-encoded ≥256-bit secret. Round-trip encode-decode is forbidden.
 */
class JwtServiceTest {

    private JwtService jwtService;

    /** A 32-byte (256-bit) random key, Base64-encoded — what production should use. */
    private static final String VALID_BASE64_SECRET =
            Base64.getEncoder().encodeToString(new byte[]{
                    1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16,
                    17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32
            });

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", VALID_BASE64_SECRET);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 60_000L);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiration", 60_000L);
    }

    @Test
    @DisplayName("audit S2 regression: signs + verifies a token using a Base64-decoded secret")
    void signsAndVerifiesWithBase64Secret() {
        User user = new Student();
        user.setId(42L);
        user.setEmail("test@example.com");
        user.setRole(Role.STUDENT);

        String token = jwtService.generateAccessToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractSubject(token)).isEqualTo("test@example.com");
        assertThat(jwtService.extractUserId(token)).isEqualTo(42L);
        assertThat(jwtService.extractRole(token)).isEqualTo("STUDENT");
    }

    @Test
    @DisplayName("audit S2 regression: tokens signed with a different key fail validation")
    void rejectsTokenSignedWithDifferentKey() {
        User user = new Student();
        user.setId(1L);
        user.setEmail("a@b.com");
        user.setRole(Role.STUDENT);

        String token = jwtService.generateAccessToken(user);

        // Rotate the secret to a different valid Base64 key.
        String differentSecret = Base64.getEncoder().encodeToString(new byte[]{
                32, 31, 30, 29, 28, 27, 26, 25, 24, 23, 22, 21, 20, 19, 18, 17,
                16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1
        });
        ReflectionTestUtils.setField(jwtService, "secretKey", differentSecret);

        // Validating the old token with the new secret must fail.
        assertThat(jwtService.isTokenValid(token)).isFalse();
        assertThatThrownBy(() -> jwtService.extractSubject(token))
                .isInstanceOf(Exception.class);
    }
}
