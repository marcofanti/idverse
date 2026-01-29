package org.itnaf.idverse.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itnaf.idverse.model.VerificationRecord;
import org.itnaf.idverse.model.VerificationRequest;
import org.itnaf.idverse.model.VerificationResponse;
import org.itnaf.idverse.repository.VerificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdVerificationService {

    private final VerificationRepository verificationRepository;
    private final WebClient webClient;
    private final OAuthTokenService oAuthTokenService;
    private final String idverseApiUrl;
    private final String verboseMode;

    public VerificationResponse verify(VerificationRequest request) {
        log.info("=== Starting Verification Process ===");
        log.info("Phone Code: {}", request.getPhoneCode());
        log.info("Phone Number: {}", request.getPhoneNumber());
        log.info("Reference ID: {}", request.getReferenceId());

        // Ensure transactionId is set (generate if empty/null)
        ensureTransactionId(request);
        log.info("Transaction ID: {}", request.getTransactionId());

        // Ensure transaction field has random suffix (min 12 chars)
        ensureTransaction(request);
        if (request.getTransaction() != null) {
            log.info("Transaction: {}", request.getTransaction());
        }

        log.debug("Request Object: {}", request);

        // Combine phone code and number for storage
        String fullPhoneNumber = request.getPhoneCode() + request.getPhoneNumber();

        VerificationRecord record = VerificationRecord.builder()
                .phoneNumber(fullPhoneNumber)
                .referenceId(request.getReferenceId())
                .transactionId(request.getTransactionId())
                .build();

        try {
            log.debug("Calling external IDVerse API...");
            String apiResponse = callExternalApi(request);

            record.setApiResponse(apiResponse);
            record.setStatus("SUCCESS");

            log.info("✓ Verification successful for reference: {}", request.getReferenceId());
            log.debug("API Response received: {}", apiResponse);

        } catch (Exception e) {
            log.error("✗ Verification failed for reference: {}", request.getReferenceId());
            log.error("Error details: {}", e.getMessage(), e);

            record.setApiResponse(null);
            record.setStatus("FAILURE");
            record.setErrorMessage(e.getMessage());
        }

        log.debug("Saving verification record to database...");
        VerificationRecord savedRecord = verificationRepository.save(record);
        log.info("Verification record saved with ID: {}", savedRecord.getId());
        log.info("=== Verification Process Complete ===");

        return mapToResponse(savedRecord);
    }

    private String callExternalApi(VerificationRequest request) {
        try {
            log.debug("=== Preparing API Call to IDVerse ===");

            // Get OAuth token first
            log.debug("Step 1: Obtaining OAuth access token...");
            String accessToken = oAuthTokenService.getAccessToken();
            String maskedToken = accessToken.substring(0, Math.min(20, accessToken.length())) + "...";
            log.debug("✓ Access token obtained: {}", maskedToken);

            // Prepare request body
            java.util.HashMap<String, String> requestBody = new java.util.HashMap<>();
            requestBody.put("phoneCode", request.getPhoneCode());
            requestBody.put("phoneNumber", request.getPhoneNumber());
            requestBody.put("referenceId", request.getReferenceId());
            requestBody.put("transactionId", request.getTransactionId());

            // Add optional fields if present
            if (request.getTransaction() != null && !request.getTransaction().trim().isEmpty()) {
                requestBody.put("transaction", request.getTransaction());
            }
            if (request.getName() != null && !request.getName().trim().isEmpty()) {
                requestBody.put("name", request.getName());
            }
            if (request.getSuppliedFirstName() != null && !request.getSuppliedFirstName().trim().isEmpty()) {
                requestBody.put("suppliedFirstName", request.getSuppliedFirstName());
            }

            log.debug("Step 2: Preparing API request");

            // If VERBOSE=SECRET, log complete POST request with all keys to console
            if ("SECRET".equalsIgnoreCase(verboseMode)) {
                System.out.println("=== COMPLETE POST REQUEST (SECRET MODE) ===");
                System.out.println("URL: " + idverseApiUrl);
                System.out.println("Method: POST");
                System.out.println("Request Headers:");
                System.out.println("  Content-Type: application/json");
                System.out.println("  Accept: application/json");
                System.out.println("  Authorization: Bearer " + accessToken);
                System.out.println("Request Body:");
                System.out.println("  phoneCode: " + request.getPhoneCode());
                System.out.println("  phoneNumber: " + request.getPhoneNumber());
                System.out.println("  referenceId: " + request.getReferenceId());
                System.out.println("  transactionId: " + request.getTransactionId());
                if (request.getTransaction() != null) System.out.println("  transaction: " + request.getTransaction());
                if (request.getName() != null) System.out.println("  name: " + request.getName());
                if (request.getSuppliedFirstName() != null) System.out.println("  suppliedFirstName: " + request.getSuppliedFirstName());
                System.out.println("===========================================");
            } else {
                log.debug("API URL: {}", idverseApiUrl);
                log.debug("HTTP Method: POST");
                log.debug("Request Headers:");
                log.debug("  - Content-Type: application/json");
                log.debug("  - Accept: application/json");
                log.debug("  - Authorization: Bearer {}", maskedToken);
                log.debug("Request Body:");
                log.debug("  - phoneCode: {}", request.getPhoneCode());
                log.debug("  - phoneNumber: {}", request.getPhoneNumber());
                log.debug("  - referenceId: {}", request.getReferenceId());
                log.debug("  - transactionId: {}", request.getTransactionId());
                if (request.getTransaction() != null) log.debug("  - transaction: {}", request.getTransaction());
                if (request.getName() != null) log.debug("  - name: {}", request.getName());
                if (request.getSuppliedFirstName() != null) log.debug("  - suppliedFirstName: {}", request.getSuppliedFirstName());
            }

            log.debug("Step 3: Sending HTTP request to IDVerse API...");
            String response = webClient.post()
                    .uri(idverseApiUrl)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("accept", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            log.debug("✓ Received response from IDVerse API");
            log.debug("Response body: {}", response);

            // Validate that response is JSON, not HTML error page
            if (response != null && response.trim().startsWith("<!DOCTYPE") ||
                (response != null && response.trim().startsWith("<html"))) {
                log.error("✗ API returned HTML instead of JSON - this indicates an error");
                log.error("HTML Response: {}", response.substring(0, Math.min(200, response.length())));
                log.error("=== API Call Failed ===");
                throw new RuntimeException("API returned HTML error page instead of JSON response");
            }

            log.debug("=== API Call Complete ===");

            return response != null ? response : "{}";

        } catch (WebClientResponseException e) {
            log.error("=== API Call Failed ===");
            log.error("HTTP Status Code: {}", e.getStatusCode());
            log.error("Status Text: {}", e.getStatusText());
            log.error("Response Headers: {}", e.getHeaders());
            log.error("Response Body: {}", e.getResponseBodyAsString());
            log.error("========================");

            // Truncate error message for 422 status
            String errorMessage;
            if (e.getStatusCode().value() == 422) {
                String responseBody = e.getResponseBodyAsString();
                errorMessage = truncateErrorMessage(responseBody, 200);
                throw new RuntimeException("API call failed with status " + e.getStatusCode() + ": " + errorMessage);
            } else {
                throw new RuntimeException("API call failed with status " + e.getStatusCode() + ": " + e.getResponseBodyAsString());
            }
        } catch (RuntimeException e) {
            // Re-throw our own RuntimeExceptions (like HTML validation error) without wrapping
            log.error("=== Unexpected Error During API Call ===");
            log.error("Exception Type: {}", e.getClass().getName());
            log.error("Error Message: {}", e.getMessage());
            log.error("=======================================", e);
            throw e;
        } catch (Exception e) {
            log.error("=== Unexpected Error During API Call ===");
            log.error("Exception Type: {}", e.getClass().getName());
            log.error("Error Message: {}", e.getMessage());
            log.error("=======================================", e);
            throw new RuntimeException("Failed to call external API: " + e.getMessage(), e);
        }
    }

    public List<VerificationResponse> getAllVerifications() {
        return verificationRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public Optional<VerificationResponse> getVerificationById(Long id) {
        return verificationRepository.findById(id)
                .map(this::mapToResponse);
    }

    private VerificationResponse mapToResponse(VerificationRecord record) {
        return VerificationResponse.builder()
                .id(record.getId())
                .phoneNumber(record.getPhoneNumber())
                .referenceId(record.getReferenceId())
                .transactionId(record.getTransactionId())
                .apiResponse(record.getApiResponse())
                .status(record.getStatus())
                .timestamp(record.getTimestamp())
                .errorMessage(record.getErrorMessage())
                .build();
    }

    /**
     * Ensures that the request has a valid transactionId.
     * If the transactionId is null or empty, generates a random one.
     * Generated IDs are 20 characters long and contain alphanumeric characters and hyphens.
     */
    private void ensureTransactionId(VerificationRequest request) {
        if (request.getTransactionId() == null || request.getTransactionId().trim().isEmpty()) {
            String generatedId = generateTransactionId();
            request.setTransactionId(generatedId);
            log.debug("Generated transaction ID: {}", generatedId);
        }
    }

    /**
     * Generates a random transaction ID.
     * Format: txn-{timestamp}-{random} (e.g., txn-1706471234567-a3f9b2)
     * Length: 26 characters (guaranteed to be between 10 and 128)
     */
    private String generateTransactionId() {
        long timestamp = System.currentTimeMillis();
        String randomPart = generateRandomAlphanumeric(6);
        return String.format("txn-%d-%s", timestamp, randomPart);
    }

    /**
     * Generates a random alphanumeric string of specified length.
     */
    private String generateRandomAlphanumeric(int length) {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Ensures that the request has a valid transaction field with random suffix.
     * If the transaction is null or empty, does nothing.
     * If provided, appends a 4-character random string (or enough to reach min 12 chars).
     */
    private void ensureTransaction(VerificationRequest request) {
        if (request.getTransaction() == null || request.getTransaction().trim().isEmpty()) {
            return; // Leave it empty if not provided
        }

        String transaction = request.getTransaction().trim();
        int currentLength = transaction.length();
        int minLength = 12;

        // Calculate how many random characters needed (minimum 4)
        int randomCharsNeeded = Math.max(4, minLength - currentLength);

        String randomSuffix = generateRandomAlphanumeric(randomCharsNeeded);
        String finalTransaction = transaction + "-" + randomSuffix;

        request.setTransaction(finalTransaction);
        log.debug("Transaction with random suffix: {}", finalTransaction);
    }

    /**
     * Truncates error message to specified length, adding ellipsis if truncated.
     */
    private String truncateErrorMessage(String message, int maxLength) {
        if (message == null || message.length() <= maxLength) {
            return message;
        }
        return message.substring(0, maxLength) + "... [truncated]";
    }
}
