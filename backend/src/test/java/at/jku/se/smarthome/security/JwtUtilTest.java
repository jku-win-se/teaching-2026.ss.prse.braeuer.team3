package at.jku.se.smarthome.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JwtUtil}.
 *
 * <p>Verifies that tokens are correctly generated, parsed, and validated.</p>
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        // Secret must be at least 32 characters for HMAC-SHA
        jwtUtil = new JwtUtil("test-secret-key-that-is-long-enough-32chars!");
    }

    @Test
    @DisplayName("generateToken: gibt nicht-leeren Token zurück")
    void generateToken_returnsNonBlankToken() {
        String token = jwtUtil.generateToken("alice@example.com");
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("extractEmail: liest korrekte E-Mail aus Token")
    void extractEmail_returnsCorrectEmail() {
        String token = jwtUtil.generateToken("alice@example.com");
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("isValid: gültiger Token → true")
    void isValid_withValidToken_returnsTrue() {
        String token = jwtUtil.generateToken("alice@example.com");
        assertThat(jwtUtil.isValid(token)).isTrue();
    }

    @Test
    @DisplayName("isValid: manipulierter Token → false")
    void isValid_withTamperedToken_returnsFalse() {
        String token = jwtUtil.generateToken("alice@example.com");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThat(jwtUtil.isValid(tampered)).isFalse();
    }

    @Test
    @DisplayName("isValid: leerer String → false")
    void isValid_withEmptyString_returnsFalse() {
        assertThat(jwtUtil.isValid("")).isFalse();
    }

    @Test
    @DisplayName("isValid: zufälliger String → false")
    void isValid_withRandomString_returnsFalse() {
        assertThat(jwtUtil.isValid("not.a.jwt.token")).isFalse();
    }

    @Test
    @DisplayName("generateToken: verschiedene E-Mails → verschiedene Token")
    void generateToken_differentEmailsProduceDifferentTokens() {
        String token1 = jwtUtil.generateToken("alice@example.com");
        String token2 = jwtUtil.generateToken("bob@example.com");
        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    @DisplayName("generateToken: Token enthält korrekten Subject")
    void generateToken_containsCorrectSubject() {
        String email = "test@smarthome.at";
        String token = jwtUtil.generateToken(email);
        assertThat(jwtUtil.extractEmail(token)).isEqualTo(email);
    }
}
