package pl.patryk.budget_api.common;

import java.time.OffsetDateTime;
import java.util.Map;

public record ErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String code,
        String message,
        Map<String, String> details
) {
    static ErrorResponse of(int status, String code, String message, Map<String, String> details) {
        return new ErrorResponse(OffsetDateTime.now(), status, code, message, details);
    }
}
