package org.itnaf.idverse.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itnaf.idverse.model.VerificationRequest;
import org.itnaf.idverse.model.VerificationResponse;
import org.itnaf.idverse.service.IdVerificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ApiController {

    private final IdVerificationService verificationService;

    @PostMapping("/verify")
    public ResponseEntity<Map<String, String>> verify(@Valid @RequestBody VerificationRequest request) {
        log.info("Received API verification request: {}", request);

        VerificationResponse response = verificationService.verify(request);

        Map<String, String> result = new HashMap<>();

        if ("SUCCESS".equals(response.getStatus())) {
            log.info("Verification successful - returning success response");
            result.put("status", "success");
            return ResponseEntity.ok(result);
        } else {
            log.error("Verification failed - returning error response");
            result.put("status", "error");
            result.put("message", response.getErrorMessage() != null ? response.getErrorMessage() : "Verification failed");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
    }

    @GetMapping("/verifications")
    public ResponseEntity<List<VerificationResponse>> getAllVerifications() {
        log.info("Fetching all verifications");
        List<VerificationResponse> verifications = verificationService.getAllVerifications();
        return ResponseEntity.ok(verifications);
    }

    @GetMapping("/verifications/{id}")
    public ResponseEntity<VerificationResponse> getVerificationById(@PathVariable Long id) {
        log.info("Fetching verification by ID: {}", id);

        return verificationService.getVerificationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> response = new HashMap<>();
        response.put("status", "error");

        StringBuilder errorMessage = new StringBuilder();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errorMessage.append(fieldName).append(": ").append(message).append("; ");
        });

        response.put("message", errorMessage.toString().trim());

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);

        Map<String, String> error = new HashMap<>();
        error.put("status", "error");
        error.put("message", ex.getMessage() != null ? ex.getMessage() : "Internal server error");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
