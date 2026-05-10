package pse.trippy.aiservice.logging;

import java.util.UUID;
import java.util.regex.Pattern;

public final class CorrelationIds {

    public static final String HEADER_NAME = "X-Correlation-ID";
    public static final String MDC_KEY = "correlationId";

    private static final int MAX_LENGTH = 128;
    private static final Pattern SAFE_VALUE = Pattern.compile("[A-Za-z0-9._:-]+");

    private CorrelationIds() {
    }

    public static String resolve(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return UUID.randomUUID().toString();
        }

        String trimmed = candidate.trim();
        if (trimmed.length() > MAX_LENGTH || !SAFE_VALUE.matcher(trimmed).matches()) {
            return UUID.randomUUID().toString();
        }

        return trimmed;
    }
}
