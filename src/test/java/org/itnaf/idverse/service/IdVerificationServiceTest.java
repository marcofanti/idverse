package org.itnaf.idverse.service;

import org.itnaf.idverse.client.IdVerseApiClient;
import org.itnaf.idverse.client.model.VerificationRequest;
import org.itnaf.idverse.model.StatusResponse;
import org.itnaf.idverse.model.VerificationRecord;
import org.itnaf.idverse.model.VerificationResponse;
import org.itnaf.idverse.repository.VerificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdVerificationServiceTest {

    private IdVerificationService idVerificationService;

    @Mock
    private VerificationRepository verificationRepository;

    @Mock
    private IdVerseApiClient idVerseApiClient;

    @BeforeEach
    void setUp() {
        idVerificationService = new IdVerificationService(
            verificationRepository,
            idVerseApiClient
        );
    }

    @Test
    void verify_shouldSucceedWithValidJsonResponse() {
        // Given
        String jsonResponse = "{\"status\":\"success\",\"transactionId\":\"txn-123\"}";
        when(idVerseApiClient.sendVerification(any())).thenReturn(jsonResponse);
        when(verificationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        VerificationRequest request = new VerificationRequest();
        request.setPhoneCode("+1");
        request.setPhoneNumber("9412607454");
        request.setReferenceId("test-ref-123");

        // When
        VerificationResponse response = idVerificationService.verify(request);

        // Then
        assertNotNull(response);
        assertEquals("SMS SENT", response.getStatus());
    }

    @Test
    void verify_shouldFailWhenApiThrowsException() {
        // Given - API client throws exception (e.g. HTML error page detected)
        when(idVerseApiClient.sendVerification(any()))
            .thenThrow(new RuntimeException("API returned HTML error page instead of JSON response"));
        when(verificationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        VerificationRequest request = new VerificationRequest();
        request.setPhoneCode("+1");
        request.setPhoneNumber("9412607454");
        request.setReferenceId("test-ref-123");

        // When
        VerificationResponse response = idVerificationService.verify(request);

        // Then
        assertNotNull(response);
        assertEquals("FAILURE", response.getStatus(),
            "Status should be FAILURE when API throws exception");
        assertNotNull(response.getErrorMessage(), "Error message should be set");
        assertTrue(response.getErrorMessage().contains("HTML error page"),
            "Error message should describe the failure");
    }

    @Test
    void verify_shouldGenerateTransactionIdWhenNotProvided() {
        // Given - No transactionId provided
        when(idVerseApiClient.sendVerification(any()))
            .thenReturn("{\"status\":\"success\",\"transactionId\":\"txn-123\"}");
        when(verificationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        VerificationRequest request = new VerificationRequest();
        request.setPhoneCode("+1");
        request.setPhoneNumber("9412607454");
        request.setReferenceId("test-ref-123");
        // transactionId is null

        // When
        VerificationResponse response = idVerificationService.verify(request);

        // Then
        assertNotNull(response.getTransactionId(), "Transaction ID should be auto-generated");
        assertTrue(response.getTransactionId().startsWith("txn-"),
            "Generated transaction ID should start with 'txn-'");
        assertTrue(response.getTransactionId().length() >= 10,
            "Generated transaction ID should be at least 10 characters");
        assertTrue(response.getTransactionId().length() <= 128,
            "Generated transaction ID should not exceed 128 characters");
    }

    @Test
    void verify_shouldUseProvidedTransactionId() {
        // Given - TransactionId is provided
        String customTransactionId = "my-custom-txn-id-12345";
        when(idVerseApiClient.sendVerification(any())).thenReturn("{\"status\":\"success\"}");
        when(verificationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        VerificationRequest request = new VerificationRequest();
        request.setPhoneCode("+1");
        request.setPhoneNumber("9412607454");
        request.setReferenceId("test-ref-123");
        request.setTransactionId(customTransactionId);

        // When
        VerificationResponse response = idVerificationService.verify(request);

        // Then
        assertEquals(customTransactionId, response.getTransactionId(),
            "Should use the provided transaction ID");
    }

    @Test
    void getLatestStatusByReferenceId_shouldReturnStatusWhenRecordExists() {
        // Given
        VerificationRecord record = VerificationRecord.builder()
                .referenceId("REF123")
                .transactionId("txn-abc")
                .phoneNumber("+11234567890")
                .status("SMS SENT")
                .timestamp(LocalDateTime.of(2026, 2, 18, 12, 0))
                .build();
        when(verificationRepository.findTopByReferenceIdOrderByTimestampDesc("REF123"))
                .thenReturn(Optional.of(record));

        // When
        Optional<StatusResponse> result = idVerificationService.getLatestStatusByReferenceId("REF123");

        // Then
        assertTrue(result.isPresent());
        assertEquals("SMS SENT", result.get().getStatus());
        assertEquals(LocalDateTime.of(2026, 2, 18, 12, 0), result.get().getTimestamp());
        assertNull(result.get().getErrorMessage());
    }

    @Test
    void getLatestStatusByReferenceId_shouldReturnEmptyWhenNotFound() {
        // Given
        when(verificationRepository.findTopByReferenceIdOrderByTimestampDesc("UNKNOWN"))
                .thenReturn(Optional.empty());

        // When
        Optional<StatusResponse> result = idVerificationService.getLatestStatusByReferenceId("UNKNOWN");

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void getLatestStatusByReferenceId_shouldIncludeErrorMessageOnFailure() {
        // Given
        VerificationRecord record = VerificationRecord.builder()
                .referenceId("REF-FAIL")
                .transactionId("txn-xyz")
                .phoneNumber("+11234567890")
                .status("FAILURE")
                .errorMessage("API returned HTML error page")
                .timestamp(LocalDateTime.of(2026, 2, 18, 12, 0))
                .build();
        when(verificationRepository.findTopByReferenceIdOrderByTimestampDesc("REF-FAIL"))
                .thenReturn(Optional.of(record));

        // When
        Optional<StatusResponse> result = idVerificationService.getLatestStatusByReferenceId("REF-FAIL");

        // Then
        assertTrue(result.isPresent());
        assertEquals("FAILURE", result.get().getStatus());
        assertEquals("API returned HTML error page", result.get().getErrorMessage());
    }

    @Test
    void getLatestStatusByTransactionId_shouldReturnStatusWhenRecordExists() {
        // Given
        VerificationRecord record = VerificationRecord.builder()
                .referenceId("REF123")
                .transactionId("txn-1234567890-abc123")
                .phoneNumber("+11234567890")
                .status("SMS SENT")
                .timestamp(LocalDateTime.of(2026, 2, 18, 12, 0))
                .build();
        when(verificationRepository.findTopByTransactionIdOrderByTimestampDesc("txn-1234567890-abc123"))
                .thenReturn(Optional.of(record));

        // When
        Optional<StatusResponse> result = idVerificationService.getLatestStatusByTransactionId("txn-1234567890-abc123");

        // Then
        assertTrue(result.isPresent());
        assertEquals("SMS SENT", result.get().getStatus());
        assertNull(result.get().getErrorMessage());
    }

    @Test
    void getLatestStatusByTransactionId_shouldReturnEmptyWhenNotFound() {
        // Given
        when(verificationRepository.findTopByTransactionIdOrderByTimestampDesc("txn-unknown"))
                .thenReturn(Optional.empty());

        // When
        Optional<StatusResponse> result = idVerificationService.getLatestStatusByTransactionId("txn-unknown");

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void verify_shouldGenerateTransactionIdWhenEmpty() {
        // Given - Empty transactionId provided
        when(idVerseApiClient.sendVerification(any())).thenReturn("{\"status\":\"success\"}");
        when(verificationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        VerificationRequest request = new VerificationRequest();
        request.setPhoneCode("+1");
        request.setPhoneNumber("9412607454");
        request.setReferenceId("test-ref-123");
        request.setTransactionId("   "); // Empty/whitespace only

        // When
        VerificationResponse response = idVerificationService.verify(request);

        // Then
        assertNotNull(response.getTransactionId(), "Transaction ID should be auto-generated when empty");
        assertTrue(response.getTransactionId().startsWith("txn-"),
            "Generated transaction ID should start with 'txn-'");
        assertNotEquals("   ", response.getTransactionId().trim(),
            "Should replace empty string with generated ID");
    }
}
