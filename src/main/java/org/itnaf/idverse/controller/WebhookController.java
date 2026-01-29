package org.itnaf.idverse.controller;

import io.jsonwebtoken.JwtException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itnaf.idverse.model.WebhookPayload;
import org.itnaf.idverse.service.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for handling webhook callbacks from IDVerse API.
 * This endpoint is protected by JWT authentication.
 */
@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final JwtService jwtService;

    /**
     * Webhook endpoint for receiving event and completion notifications from IDVerse API.
     * Protected by JWT Bearer token authentication.
     *
     * @param authHeader Authorization header containing Bearer token
     * @param payload Webhook payload with transactionId and event
     * @return ResponseEntity with success/error message
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> handleWebhook(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody WebhookPayload payload) {

        log.info("=== Webhook Request Received ===");
        log.info("Transaction ID: {}", payload.getTransactionId());
        log.info("Event: {}", payload.getEvent());

        // Validate Authorization header
        if (authHeader == null || authHeader.isEmpty()) {
            log.warn("Missing Authorization header");
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "error", "Unauthorized",
                            "message", "Missing Authorization header"
                    ));
        }

        // Extract Bearer token
        if (!authHeader.startsWith("Bearer ")) {
            log.warn("Invalid Authorization header format: {}", authHeader);
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "error", "Unauthorized",
                            "message", "Authorization header must start with 'Bearer '"
                    ));
        }

        String token = authHeader.substring(7); // Remove "Bearer " prefix

        // Validate JWT token
        try {
            jwtService.validateToken(token);
            log.info("JWT token validated successfully");
        } catch (JwtException e) {
            log.error("JWT validation failed: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "error", "Unauthorized",
                            "message", "Invalid or expired token: " + e.getMessage()
                    ));
        }

        // Process webhook payload
        log.info("Processing webhook event: {} for transaction: {}", payload.getEvent(), payload.getTransactionId());

        // TODO: Add business logic here to handle different event types
        // For example:
        // - Update verification record status
        // - Send notifications
        // - Trigger workflows based on event type

        switch (payload.getEvent().toLowerCase()) {
            case "pending":
                log.info("Transaction link sent to end-user");
                break;
            case "termsandconditions":
                log.info("End-user reviewing Terms and Conditions");
                break;
            case "idselection":
                log.info("End-user selecting ID for verification");
                break;
            case "personaldetails":
                log.info("End-user checking extracted information");
                break;
            case "liveness":
                log.info("End-user at liveness attempt phase");
                break;
            case "completedpass":
                log.info("Verification completed - PASSED");
                break;
            case "completedflagged":
                log.warn("Verification completed - FLAGGED (conflicts detected)");
                break;
            case "expired":
                log.warn("Transaction EXPIRED");
                break;
            case "cancelled":
                log.warn("Transaction CANCELLED");
                break;
            default:
                log.warn("Unknown event type: {}", payload.getEvent());
        }

        log.info("=== Webhook Request Processed Successfully ===");

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Webhook received and processed",
                "transactionId", payload.getTransactionId(),
                "event", payload.getEvent()
        ));
    }

    /**
     * Exception handler for validation errors.
     */
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(
            org.springframework.web.bind.MethodArgumentNotValidException ex) {

        log.error("Webhook validation error: {}", ex.getMessage());

        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + ", " + b)
                .orElse("Validation failed");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", "Validation Error",
                        "message", errorMessage
                ));
    }
}
