package org.itnaf.idverse.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itnaf.idverse.model.UpdateStatusRequest;
import org.itnaf.idverse.model.VerificationRecord;
import org.itnaf.idverse.repository.VerificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Unauthenticated status-update endpoint.
 *
 * Mirrors the webhook logic but requires no JWT token — intended for
 * testing, internal tooling, and client-side status simulation.
 */
@RestController
@RequestMapping("/api/updateStatus")
@RequiredArgsConstructor
@Slf4j
public class UpdateStatusController {

    private final VerificationRepository verificationRepository;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<Map<String, String>> updateStatus(@Valid @RequestBody UpdateStatusRequest request) {
        log.info("=== Update Status Request Received ===");
        log.info("Transaction ID: {}", request.getTransactionId());
        log.info("Event: {}, Status: {}", request.getEvent(), request.getStatus());

        // Require at least one of event or status
        boolean hasEvent = request.getEvent() != null && !request.getEvent().isBlank();
        boolean hasStatus = request.getStatus() != null && !request.getStatus().isBlank();

        if (!hasEvent && !hasStatus) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "error", "Validation Error",
                    "message", "At least one of 'event' or 'status' must be provided"
            ));
        }

        // Resolve the status value — event takes precedence
        String resolvedStatus = hasEvent
                ? convertEventToStatus(request.getEvent())
                : request.getStatus();

        log.info("Resolved status: {}", resolvedStatus);

        // Look up existing record for this transaction ID
        List<VerificationRecord> existing = verificationRepository
                .findByTransactionIdAndStatusOrderByTimestampDesc(request.getTransactionId(), "SMS SENT");
        if (existing.isEmpty()) {
            existing = verificationRepository
                    .findByTransactionIdAndStatusOrderByTimestampDesc(request.getTransactionId(), "FAILURE");
        }

        // Build new record
        VerificationRecord newRecord = new VerificationRecord();
        newRecord.setTransactionId(request.getTransactionId());
        newRecord.setStatus(resolvedStatus);

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            payloadJson = "{\"transactionId\":\"" + request.getTransactionId() + "\"}";
        }
        newRecord.setApiResponse(payloadJson);

        if (!existing.isEmpty()) {
            VerificationRecord source = existing.get(0);
            newRecord.setPhoneNumber(source.getPhoneNumber());
            newRecord.setReferenceId(source.getReferenceId());
            log.info("Copied phoneNumber and referenceId from existing record");
        } else {
            log.warn("Transaction ID {} not found — saving record without phoneNumber/referenceId",
                    request.getTransactionId());
            newRecord.setErrorMessage("Transaction ID not found at time of status update");
        }

        VerificationRecord saved = verificationRepository.save(newRecord);
        log.info("Saved new record ID {} with status '{}'", saved.getId(), resolvedStatus);
        log.info("=== Update Status Request Processed ===");

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Status updated",
                "transactionId", request.getTransactionId(),
                "resolvedStatus", resolvedStatus,
                "recordId", String.valueOf(saved.getId())
        ));
    }

    /**
     * Converts a camelCase IDVerse event name to UPPERCASE WITH SPACES.
     * E.g. "completedPass" → "COMPLETED PASS", "pending" → "PENDING"
     */
    private String convertEventToStatus(String event) {
        if (event == null || event.isEmpty()) return "";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < event.length(); i++) {
            char c = event.charAt(i);
            if (i > 0 && Character.isUpperCase(c)) result.append(' ');
            result.append(Character.toUpperCase(c));
        }
        return result.toString();
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(
            org.springframework.web.bind.MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce((a, b) -> a + ", " + b)
                .orElse("Validation failed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "Validation Error",
                "message", message
        ));
    }
}
