package org.itnaf.idverse.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.itnaf.idverse.client.model.VerificationRequest;
import org.itnaf.idverse.model.VerificationResponse;
import org.itnaf.idverse.service.IdVerificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApiController.class)
class ApiControllerVerifyTestEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IdVerificationService verificationService;

    private VerificationRequest validRequest() {
        VerificationRequest req = new VerificationRequest();
        req.setPhoneCode("+1");
        req.setPhoneNumber("9412607454");
        req.setReferenceId("REF-TEST-001");
        req.setTransactionId("txn-test-1234567");
        return req;
    }

    private VerificationResponse successResponse() {
        return VerificationResponse.builder()
                .id(1L)
                .phoneNumber("+19412607454")
                .referenceId("REF-TEST-001")
                .transactionId("txn-test-1234567")
                .apiResponse("{\"mock\":true,\"status\":\"success\"}")
                .status("SMS SENT")
                .timestamp(LocalDateTime.of(2026, 2, 20, 12, 0, 0))
                .build();
    }

    // --- dryRun=true ---

    @Test
    void verifyTest_dryRunTrue_shouldReturn200AndNotCallRealApi() throws Exception {
        when(verificationService.verify(any(), eq(true))).thenReturn(successResponse());

        mockMvc.perform(post("/api/verify/test?dryRun=true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.dryRun").value("true"))
                .andExpect(jsonPath("$.transactionId").value("txn-test-1234567"));

        verify(verificationService).verify(any(), eq(true));
    }

    @Test
    void verifyTest_dryRunTrue_shouldIncludeTransactionId() throws Exception {
        VerificationResponse response = successResponse();
        response.setTransactionId("txn-generated-abc123");
        when(verificationService.verify(any(), eq(true))).thenReturn(response);

        mockMvc.perform(post("/api/verify/test?dryRun=true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("txn-generated-abc123"));
    }

    // --- dryRun=false (real call) ---

    @Test
    void verifyTest_dryRunFalse_shouldReturn200OnSuccess() throws Exception {
        when(verificationService.verify(any(), eq(false))).thenReturn(successResponse());

        mockMvc.perform(post("/api/verify/test?dryRun=false")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.dryRun").value("false"));

        verify(verificationService).verify(any(), eq(false));
    }

    @Test
    void verifyTest_defaultDryRun_shouldBeFalse() throws Exception {
        when(verificationService.verify(any(), eq(false))).thenReturn(successResponse());

        // No dryRun param — should default to false
        mockMvc.perform(post("/api/verify/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dryRun").value("false"));

        verify(verificationService).verify(any(), eq(false));
    }

    @Test
    void verifyTest_dryRunFalse_shouldReturn400OnFailure() throws Exception {
        VerificationResponse failure = VerificationResponse.builder()
                .status("FAILURE")
                .errorMessage("API returned HTML error page")
                .transactionId("txn-test-1234567")
                .timestamp(LocalDateTime.of(2026, 2, 20, 12, 0, 0))
                .build();
        when(verificationService.verify(any(), eq(false))).thenReturn(failure);

        mockMvc.perform(post("/api/verify/test?dryRun=false")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("API returned HTML error page"));
    }

    // --- Validation ---

    @Test
    void verifyTest_missingRequiredFields_shouldReturn400() throws Exception {
        VerificationRequest incomplete = new VerificationRequest();
        // phoneCode, phoneNumber, referenceId all missing

        mockMvc.perform(post("/api/verify/test?dryRun=true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incomplete)))
                .andExpect(status().isBadRequest());
    }
}
