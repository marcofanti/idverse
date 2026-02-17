package org.itnaf.idverse.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itnaf.idverse.client.IdVerseApiClient;
import org.itnaf.idverse.client.model.VerificationRequest;
import org.itnaf.idverse.model.VerificationRecord;
import org.itnaf.idverse.model.VerificationResponse;
import org.itnaf.idverse.repository.VerificationRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdVerificationService {

    private final VerificationRepository verificationRepository;
    private final IdVerseApiClient idVerseApiClient;

    public VerificationResponse verify(VerificationRequest request) {
        log.info("=== Starting Verification Process ===");
        log.info("Phone Code: {}", request.getPhoneCode());
        log.info("Phone Number: {}", request.getPhoneNumber());
        log.info("Reference ID: {}", request.getReferenceId());

        // Ensure transactionId is set (generate if empty/null)
        ensureTransactionId(request);
        log.info("Transaction ID: {}", request.getTransactionId());

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
            String apiResponse = idVerseApiClient.sendVerification(request);

            record.setApiResponse(apiResponse);
            record.setStatus("SMS SENT");

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
     */
    private String generateTransactionId() {
        long timestamp = System.currentTimeMillis();
        String randomPart = generateRandomAlphanumeric(6);
        return String.format("txn-%d-%s", timestamp, randomPart);
    }

    private String generateRandomAlphanumeric(int length) {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
