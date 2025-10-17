package com.lloyds.transaction.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import javax.crypto.SecretKey;
import java.util.Base64;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    @InjectMocks
    private JwtUtil jwtUtil;

    private static final String SECRET_KEY = "mysecretkeymysecretkeymysecretkeymysecretkey";
    private static final int CUSTOMER_ID = 123456;
    private String validToken;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET_KEY);
        validToken = generateValidToken(CUSTOMER_ID);
    }

    private String generateValidToken(int customerId) {
        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET_KEY));
        return Jwts.builder()
                .setSubject(String.valueOf(customerId))
                .signWith(key)
                .compact();
    }

    @Test
    void testExtractId() {
        String extractedId = jwtUtil.extractId(validToken);
        assertThat(extractedId).isEqualTo(String.valueOf(CUSTOMER_ID));
    }

    @Test
    void testExtractJti() {
        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET_KEY));
        String expectedJti = "test-jti";
        String tokenWithJti = Jwts.builder()
                .setSubject(String.valueOf(CUSTOMER_ID))
                .setId(expectedJti) // Add JTI claim
                .signWith(key)
                .compact();

        String extractedJti = jwtUtil.extractJti(tokenWithJti);

        assertThat(extractedJti).isEqualTo(expectedJti);
    }


    @Test
    void testInvalidToken() {
        String invalidToken = validToken + "invalid";
        assertThrows(Exception.class, () -> jwtUtil.extractAllClaims(invalidToken));
    }

    @Test
    void testIsTokenValid() {
        assertThat(jwtUtil.isTokenValid(validToken)).isTrue();
    }

    @Test
    void testIsTokenInvalid() {
        String invalidToken = validToken + "invalid";
        assertThat(jwtUtil.isTokenValid(invalidToken)).isFalse();
    }
}
