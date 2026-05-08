package pse.trippy.aiservice.logging;

public final class LogSanitizer {

    private static final int MAX_DETAIL_LENGTH = 180;

    private LogSanitizer() {
    }

    public static String safeDetail(String value) {
        if (value == null || value.isBlank()) {
            return "none";
        }

        String cleaned = value
                .replaceAll("[\\r\\n\\t\\x00-\\x1F\\x7F]+", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
        if (cleaned.length() <= MAX_DETAIL_LENGTH) {
            return cleaned;
        }
        return cleaned.substring(0, MAX_DETAIL_LENGTH) + "...";
    }

    public static String safeError(Throwable ex) {
        if (ex == null) {
            return "none";
        }

        String message = safeDetail(ex.getMessage());
        if ("none".equals(message)) {
            return ex.getClass().getSimpleName();
        }
        return ex.getClass().getSimpleName() + ": " + message;
    }

    public static String shortHash(String hash) {
        String cleaned = safeDetail(hash);
        if ("none".equals(cleaned)) {
            return "unknown";
        }
        return cleaned.substring(0, Math.min(12, cleaned.length()));
    }
}
