package org.itnaf.idverse.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationResponse {

    private Long id;
    private String phoneNumber;
    private String referenceId;
    private String apiResponse;
    private String status;
    private LocalDateTime timestamp;
    private String errorMessage;
}
