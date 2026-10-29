package dio.budgeting.auth.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static dio.budgeting.support.TestConstants.RAW_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;

class BCryptPasswordEncoderGatewayTest {

    private PasswordEncoder passwordEncoder;
    private BCryptPasswordEncoderGateway gateway;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        gateway = new BCryptPasswordEncoderGateway(passwordEncoder);
    }

    @Test
    @DisplayName("Given a raw password When encode is called Then the result differs from the raw password")
    void givenRawPasswordWhenEncodeIsCalledThenTheResultDiffersFromTheRawPassword() {
        final var actual = gateway.encode(RAW_PASSWORD);

        verifyGivenRawPasswordWhenEncodeIsCalledThenTheResultDiffersFromTheRawPassword(actual);
    }

    @Test
    @DisplayName("Given a raw password When encode is called Then BCrypt recognizes the raw password as a match")
    void givenRawPasswordWhenEncodeIsCalledThenBCryptRecognizesTheRawPasswordAsAMatch() {
        final var actual = gateway.encode(RAW_PASSWORD);

        verifyGivenRawPasswordWhenEncodeIsCalledThenBCryptRecognizesTheRawPasswordAsAMatch(actual);
    }

    @Test
    @DisplayName("Given the same raw password encoded twice When the results are compared Then they are different because of the salt")
    void givenSameRawPasswordEncodedTwiceWhenTheResultsAreComparedThenTheyAreDifferentBecauseOfTheSalt() {
        final var first = gateway.encode(RAW_PASSWORD);
        final var second = gateway.encode(RAW_PASSWORD);

        verifyGivenSameRawPasswordEncodedTwiceWhenTheResultsAreComparedThenTheyAreDifferentBecauseOfTheSalt(
                first, second);
    }

    private void verifyGivenRawPasswordWhenEncodeIsCalledThenTheResultDiffersFromTheRawPassword(final String actual) {
        assertThat(actual).isNotEqualTo(RAW_PASSWORD);
    }

    private void verifyGivenRawPasswordWhenEncodeIsCalledThenBCryptRecognizesTheRawPasswordAsAMatch(
            final String actual) {
        assertThat(passwordEncoder.matches(RAW_PASSWORD, actual)).isTrue();
    }

    private void verifyGivenSameRawPasswordEncodedTwiceWhenTheResultsAreComparedThenTheyAreDifferentBecauseOfTheSalt(
            final String first, final String second) {
        assertThat(first).isNotEqualTo(second);
        assertThat(passwordEncoder.matches(RAW_PASSWORD, first)).isTrue();
        assertThat(passwordEncoder.matches(RAW_PASSWORD, second)).isTrue();
    }
}
