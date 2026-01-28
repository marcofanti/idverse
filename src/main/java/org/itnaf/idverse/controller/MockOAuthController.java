package org.itnaf.idverse.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itnaf.idverse.model.OAuthTokenResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/3.5")
@RequiredArgsConstructor
@Slf4j
public class MockOAuthController {

    private final String oauthToken;

    @PostMapping("/oauthToken")
    public ResponseEntity<OAuthTokenResponse> getOAuthToken() {
        log.debug("Mock OAuth endpoint called: /api/3.5/oauthToken");

        if (oauthToken == null || oauthToken.isEmpty()) {
            log.error("OAUTHTOKEN not configured in .env file");
            OAuthTokenResponse errorResponse = new OAuthTokenResponse();
            errorResponse.setError("invalid_configuration");
            errorResponse.setErrorDescription("OAUTHTOKEN not configured");
            errorResponse.setMessage("Please set OAUTHTOKEN in your .env file");
            return ResponseEntity.status(500).body(errorResponse);
        }

        OAuthTokenResponse response = new OAuthTokenResponse();
        response.setTokenType("Bearer");
        response.setExpiresIn(900);
        response.setAccessToken(oauthToken);

        log.debug("Returning mock OAuth token (length: {} characters)", oauthToken.length());

        return ResponseEntity.ok(response);
    }
}
