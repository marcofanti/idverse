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

    public VerificationResponse verify(VerificationRequest request) {
        log.info("=== Starting Verification Process ===");
        log.info("Phone Number: {}", request.getPhoneNumber());
        log.info("Reference ID: {}", request.getReferenceId());
        log.debug("Request Object: {}", request);

        VerificationRecord record = VerificationRecord.builder()
                .phoneNumber(request.getPhoneNumber())
                .referenceId(request.getReferenceId())
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
            Map<String, String> requestBody = Map.of(
                    "phoneNumber", request.getPhoneNumber(),
                    "referenceId", request.getReferenceId()
            );

            log.debug("Step 2: Preparing API request");
            log.debug("API URL: {}", idverseApiUrl);
            log.debug("HTTP Method: POST");
            log.debug("Request Headers:");
            log.debug("  - Content-Type: application/json");
            log.debug("  - Authorization: Bearer {}", maskedToken);
            log.debug("Request Body:");
            log.debug("  - phoneNumber: {}", request.getPhoneNumber());
            log.debug("  - referenceId: {}", request.getReferenceId());

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
            log.debug("=== API Call Complete ===");

            return response != null ? response : "{}";

        } catch (WebClientResponseException e) {
            log.error("=== API Call Failed ===");
            log.error("HTTP Status Code: {}", e.getStatusCode());
            log.error("Status Text: {}", e.getStatusText());
            log.error("Response Headers: {}", e.getHeaders());
            log.error("Response Body: {}", e.getResponseBodyAsString());
            log.error("========================");
            throw new RuntimeException("API call failed with status " + e.getStatusCode() + ": " + e.getResponseBodyAsString());
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
                .apiResponse(record.getApiResponse())
                .status(record.getStatus())
                .timestamp(record.getTimestamp())
                .errorMessage(record.getErrorMessage())
                .build();
    }
}
