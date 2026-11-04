package productioneventprocessing.DTOs;

public record ValidationResult(boolean valid, String reason) {
    public static ValidationResult ok() { return new ValidationResult(true, null); }
    public static ValidationResult fail(String reason) { return new ValidationResult(false, reason); }
}
