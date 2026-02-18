package org.itnaf.idverse.controller;

import org.itnaf.idverse.model.StatusResponse;
import org.itnaf.idverse.service.IdVerificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApiController.class)
class ApiControllerStatusTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IdVerificationService verificationService;

    // --- /api/status/reference/{referenceId} ---

    @Test
    void getStatusByReferenceId_shouldReturn200WithStatus() throws Exception {
        StatusResponse status = StatusResponse.builder()
                .status("SMS SENT")
                .timestamp(LocalDateTime.of(2026, 2, 18, 12, 0, 0))
                .errorMessage(null)
                .build();
        when(verificationService.getLatestStatusByReferenceId("REF123")).thenReturn(Optional.of(status));

        mockMvc.perform(get("/api/status/reference/REF123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SMS SENT"))
                .andExpect(jsonPath("$.timestamp").value("2026-02-18T12:00:00"))
                .andExpect(jsonPath("$.errorMessage").doesNotExist());
    }

    @Test
    void getStatusByReferenceId_shouldReturn404WhenNotFound() throws Exception {
        when(verificationService.getLatestStatusByReferenceId("UNKNOWN")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/status/reference/UNKNOWN"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getStatusByReferenceId_shouldIncludeErrorMessageOnFailure() throws Exception {
        StatusResponse status = StatusResponse.builder()
                .status("FAILURE")
                .timestamp(LocalDateTime.of(2026, 2, 18, 12, 0, 0))
                .errorMessage("API returned HTML error page")
                .build();
        when(verificationService.getLatestStatusByReferenceId("REF-FAIL")).thenReturn(Optional.of(status));

        mockMvc.perform(get("/api/status/reference/REF-FAIL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILURE"))
                .andExpect(jsonPath("$.errorMessage").value("API returned HTML error page"));
    }

    // --- /api/status/transaction/{transactionId} ---

    @Test
    void getStatusByTransactionId_shouldReturn200WithStatus() throws Exception {
        StatusResponse status = StatusResponse.builder()
                .status("SMS SENT")
                .timestamp(LocalDateTime.of(2026, 2, 18, 12, 0, 0))
                .errorMessage(null)
                .build();
        when(verificationService.getLatestStatusByTransactionId("txn-1234567890-abc123"))
                .thenReturn(Optional.of(status));

        mockMvc.perform(get("/api/status/transaction/txn-1234567890-abc123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SMS SENT"))
                .andExpect(jsonPath("$.timestamp").value("2026-02-18T12:00:00"))
                .andExpect(jsonPath("$.errorMessage").doesNotExist());
    }

    @Test
    void getStatusByTransactionId_shouldReturn404WhenNotFound() throws Exception {
        when(verificationService.getLatestStatusByTransactionId("txn-unknown")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/status/transaction/txn-unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getStatusByTransactionId_shouldIncludeErrorMessageOnFailure() throws Exception {
        StatusResponse status = StatusResponse.builder()
                .status("FAILURE")
                .timestamp(LocalDateTime.of(2026, 2, 18, 12, 0, 0))
                .errorMessage("Connection timeout")
                .build();
        when(verificationService.getLatestStatusByTransactionId("txn-fail-999"))
                .thenReturn(Optional.of(status));

        mockMvc.perform(get("/api/status/transaction/txn-fail-999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILURE"))
                .andExpect(jsonPath("$.errorMessage").value("Connection timeout"));
    }
}
