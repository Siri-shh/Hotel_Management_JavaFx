package hotel.util;

import javafx.scene.control.TextFormatter;

/**
 * Utility class for input validation and TextFormatters.
 * TextFormatters are applied directly to fields so the user
 * physically cannot type invalid characters.
 */
public final class Validator {

    private Validator() {}

    // ── Type checks ───────────────────────────────────────────────────────────

    public static boolean isPositiveInt(String s) {
        if (s == null || s.isBlank()) return false;
        try { return Integer.parseInt(s.trim()) > 0; }
        catch (NumberFormatException e) { return false; }
    }

    public static boolean isPositiveDouble(String s) {
        if (s == null || s.isBlank()) return false;
        try { return Double.parseDouble(s.trim()) > 0; }
        catch (NumberFormatException e) { return false; }
    }

    /** Exactly 10 digits. */
    public static boolean isValidPhone(String s) {
        return s != null && s.matches("\\d{10}");
    }

    /** At least 2 characters after trimming. */
    public static boolean isValidName(String s) {
        return s != null && s.trim().length() >= 2;
    }

    // ── TextFormatters (applied on TextField so bad chars are blocked) ─────────

    /** Allows only digit characters, up to maxLen digits. */
    public static TextFormatter<String> intFormatter(int maxLen) {
        return new TextFormatter<>(change ->
            change.getControlNewText().matches("\\d{0," + maxLen + "}") ? change : null);
    }

    /** Allows a decimal number: up to 8 digits, optional dot, up to 2 decimals. */
    public static TextFormatter<String> decimalFormatter() {
        return new TextFormatter<>(change ->
            change.getControlNewText().matches("\\d{0,8}(\\.\\d{0,2})?") ? change : null);
    }

    /** Allows only digit characters, up to 10 (for phone numbers). */
    public static TextFormatter<String> phoneFormatter() {
        return new TextFormatter<>(change ->
            change.getControlNewText().matches("\\d{0,10}") ? change : null);
    }

    /** Exactly 12 digits — valid Aadhaar number. */
    public static boolean isValidAadhaar(String s) {
        return s != null && s.matches("\\d{12}");
    }

    /** Allows only digits, up to 12 (Aadhaar). */
    public static TextFormatter<String> aadhaarFormatter() {
        return new TextFormatter<>(change ->
            change.getControlNewText().matches("\\d{0,12}") ? change : null);
    }
}
