package pse.trippy.aiservice.logging;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogSanitizerTest {

    @Test
    void safeError_removesLineBreaksAndIncludesExceptionType() {
        IllegalArgumentException ex = new IllegalArgumentException("bad\ninput\tvalue");

        assertThat(LogSanitizer.safeError(ex))
                .isEqualTo("IllegalArgumentException: bad input value");
    }

    @Test
    void shortHash_limitsHashLength() {
        assertThat(LogSanitizer.shortHash("1234567890abcdef")).isEqualTo("1234567890ab");
    }
}
