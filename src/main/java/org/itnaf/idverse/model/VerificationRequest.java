package org.itnaf.idverse.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificationRequest {

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format. Use international format (e.g., +1234567890)")
    private String phoneNumber;

    @NotBlank(message = "Reference ID is required")
    private String referenceId;

    // Optional in form, but required in API (will be auto-generated if empty)
    @Size(min = 10, max = 128, message = "Transaction ID must be between 10 and 128 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\s_-]*$", message = "Transaction ID can only contain alphanumeric characters, spaces, hyphens, and underscores")
    private String transactionId;
}
