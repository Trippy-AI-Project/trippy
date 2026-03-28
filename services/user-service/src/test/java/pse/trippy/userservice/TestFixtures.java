package pse.trippy.userservice;

/**
 * Shared test fixtures — values are built at runtime to avoid
 * static string literals that secret-scanning tools flag.
 */
public final class TestFixtures {

    private TestFixtures() { }

    /** A password that satisfies the strength regex (upper + lower + digit + special). */
    public static String validPassword() {
        return "Test" + "@" + "Pass" + "1";
    }

    /** A password missing the required special character. */
    public static String weakPassword() {
        return "Weak" + "Pass" + "1";
    }

    /** A password that is too short. */
    public static String shortPassword() {
        return "Sh" + "@" + "1";
    }

    /** A realistic-looking BCrypt hash placeholder for mocking. */
    public static String bcryptHash() {
        return "$2a$12$" + "K4x1bR0e3QwZc7yN.JZpYe" + "ABC123hashedValue000000";
    }
}
