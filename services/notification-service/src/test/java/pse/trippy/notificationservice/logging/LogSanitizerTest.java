package pse.trippy.notificationservice.logging;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogSanitizerTest {

    @Test
    void maskEmail_masksLocalPart() {
        assertThat(LogSanitizer.maskEmail("Alice.Example@Test.COM")).isEqualTo("al***@test.com");
    }

    @Test
    void safeError_removesLineBreaksAndIncludesExceptionType() {
        IllegalArgumentException ex = new IllegalArgumentException("bad\ninput\tvalue");

        assertThat(LogSanitizer.safeError(ex))
                .isEqualTo("IllegalArgumentException: bad input value");
    }
}
