package org.itnaf.idverse.service;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.itnaf.idverse.model.VerificationRequest;
import org.itnaf.idverse.model.VerificationResponse;
import org.itnaf.idverse.repository.VerificationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdVerificationServiceTest {

    private MockWebServer mockWebServer;
    private IdVerificationService idVerificationService;

    @Mock
    private VerificationRepository verificationRepository;

    @Mock
    private OAuthTokenService oAuthTokenService;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String baseUrl = mockWebServer.url("/").toString();
        WebClient webClient = WebClient.builder().baseUrl(baseUrl).build();

        idVerificationService = new IdVerificationService(
            verificationRepository,
            webClient,
            oAuthTokenService,
            baseUrl + "api/verify",
            "DEBUG"  // verboseMode - use DEBUG for tests, not SECRET
        );

        // Mock OAuth token service to return a test token
        when(oAuthTokenService.getAccessToken()).thenReturn("test-access-token");
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    void verify_shouldIncludeAcceptJsonHeader() throws Exception {
        // Given
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("{\"status\":\"success\",\"message\":\"Verification sent\"}")
            .addHeader("Content-Type", "application/json"));

        VerificationRequest request = new VerificationRequest();
        request.setPhoneNumber("+19412607454");
        request.setReferenceId("test-ref-123");

        // When
        try {
            idVerificationService.verify(request);
        } catch (Exception e) {
            // Exception expected due to mock repository, but we can still check the request
        }

        // Then - Verify the Accept header was sent
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("application/json", recordedRequest.getHeader("accept"),
            "Request should include Accept: application/json header");
        assertEquals("Bearer test-access-token", recordedRequest.getHeader("Authorization"),
            "Request should include Authorization header");
    }

    @Test
    void verify_shouldFailWhenApiReturnsHtml() {
        // Given - API returns HTML error page instead of JSON
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("<!DOCTYPE html><html><head><title>Error</title></head><body>Error</body></html>")
            .addHeader("Content-Type", "text/html"));

        VerificationRequest request = new VerificationRequest();
        request.setPhoneNumber("+19412607454");
        request.setReferenceId("test-ref-123");

        // Mock repository save
        when(verificationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        VerificationResponse response = idVerificationService.verify(request);

        // Then
        assertNotNull(response);
        assertEquals("FAILURE", response.getStatus(),
            "Status should be FAILURE when HTML is returned instead of JSON");
        assertNotNull(response.getErrorMessage(), "Error message should be set");
        assertTrue(response.getErrorMessage().contains("HTML error page"),
            "Error message should indicate HTML was returned instead of JSON");
    }

    @Test
    void verify_shouldFailWhenApiReturnsHtmlWithLowercaseDoctype() {
        // Given - API returns HTML with lowercase doctype
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("<html><head><title>Error</title></head><body>Not Found</body></html>")
            .addHeader("Content-Type", "text/html"));

        VerificationRequest request = new VerificationRequest();
        request.setPhoneNumber("+19412607454");
        request.setReferenceId("test-ref-123");

        // Mock repository save
        when(verificationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        VerificationResponse response = idVerificationService.verify(request);

        // Then
        assertNotNull(response);
        assertEquals("FAILURE", response.getStatus(),
            "Status should be FAILURE when HTML is returned (lowercase html tag)");
        assertNotNull(response.getErrorMessage(), "Error message should be set");
        assertTrue(response.getErrorMessage().contains("HTML error page"),
            "Error message should indicate HTML was returned instead of JSON");
    }

    @Test
    void verify_shouldSucceedWithValidJsonResponse() throws Exception {
        // Given
        String jsonResponse = "{\"status\":\"success\",\"transactionId\":\"txn-123\"}";
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(jsonResponse)
            .addHeader("Content-Type", "application/json"));

        VerificationRequest request = new VerificationRequest();
        request.setPhoneNumber("+19412607454");
        request.setReferenceId("test-ref-123");

        // Mock repository save
        when(verificationRepository.save(any())).thenAnswer(invocation -> {
            var record = invocation.getArgument(0);
            return record;
        });

        // When
        VerificationResponse response = idVerificationService.verify(request);

        // Then
        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());

        // Verify request had correct headers
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("application/json", recordedRequest.getHeader("accept"));
        assertEquals("Bearer test-access-token", recordedRequest.getHeader("Authorization"));
    }

    @Test
    void verify_shouldHandleHttpErrorStatus() {
        // Given - API returns 401 Unauthorized
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(401)
            .setBody("Unauthorized"));

        VerificationRequest request = new VerificationRequest();
        request.setPhoneNumber("+19412607454");
        request.setReferenceId("test-ref-123");

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            idVerificationService.verify(request);
        });
    }

    @Test
    void verify_shouldHandleNetworkTimeout() {
        // Given - Simulate slow response that will timeout
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("{\"status\":\"success\"}")
            .setBodyDelay(35, java.util.concurrent.TimeUnit.SECONDS)); // Longer than 30s timeout

        VerificationRequest request = new VerificationRequest();
        request.setPhoneNumber("+19412607454");
        request.setReferenceId("test-ref-123");

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            idVerificationService.verify(request);
        });
    }
}
