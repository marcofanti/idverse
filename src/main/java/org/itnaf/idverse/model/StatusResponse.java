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
public class StatusResponse {

    private String status;
    private LocalDateTime timestamp;
    private String errorMessage;
}
