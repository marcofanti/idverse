package org.itnaf.idverse.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for POST /api/updateStatus.
 *
 * Provide either {@code event} (camelCase IDVerse event name, e.g. "completedPass"),
 * {@code status} (explicit uppercase status string, e.g. "COMPLETED PASS"),
 * or both — {@code event} takes precedence when both are present.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStatusRequest {

    @NotBlank(message = "transactionId is required")
    private String transactionId;

    /** IDVerse camelCase event name. Takes precedence over status if both are provided. */
    private String event;

    /** Explicit status string. Used only when event is not provided. */
    private String status;
}
