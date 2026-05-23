package pse.trippy.notificationservice.logging;

import java.util.Locale;

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

    public static String maskEmail(String email) {
        String cleaned = safeDetail(email);
        if ("none".equals(cleaned)) {
            return "missing";
        }

        String normalized = cleaned.toLowerCase(Locale.ROOT);
        int atIndex = normalized.indexOf('@');
        if (atIndex <= 0 || atIndex == normalized.length() - 1) {
            return "invalid";
        }

        String local = normalized.substring(0, atIndex);
        String domain = normalized.substring(atIndex + 1);
        String prefix = local.substring(0, Math.min(2, local.length()));
        return prefix + "***@" + domain;
    }
}
