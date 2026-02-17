package org.itnaf.idverse.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itnaf.idverse.client.model.OAuthTokenResponse;
import org.itnaf.idverse.client.service.OAuthTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
@Slf4j
public class OAuthTestController {

    private final OAuthTokenService oAuthTokenService;

    @GetMapping("/oauth")
    public ResponseEntity<Map<String, Object>> testOAuthToken(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String verbose) {

        boolean isDebug = "debug".equalsIgnoreCase(verbose);

        if (isDebug) {
            log.info("Testing OAuth token retrieval (VERBOSE DEBUG MODE)");
        } else {
            log.info("Testing OAuth token retrieval");
        }

        Map<String, Object> result = new HashMap<>();

        try {
            Map<String, Object> debugInfo = oAuthTokenService.testConnectionVerbose(isDebug);

            if (isDebug) {
                // Include all debug information in the response
                result.putAll(debugInfo);
            } else {
                // Standard response without debug details
                OAuthTokenResponse response = oAuthTokenService.testConnection();

                if (response.isSuccess()) {
                    result.put("status", "SUCCESS");
                    result.put("message", "OAuth token obtained successfully");
                    result.put("token_type", response.getTokenType());
                    result.put("expires_in", response.getExpiresIn());
                    result.put("access_token_preview", response.getAccessToken().substring(0, Math.min(20, response.getAccessToken().length())) + "...");
                } else {
                    result.put("status", "FAILURE");
                    result.put("error", response.getError());
                    result.put("error_description", response.getErrorDescription());
                    result.put("hint", response.getHint());
                    result.put("message", response.getMessage());
                }
            }

            String status = (String) result.get("status");
            if ("SUCCESS".equals(status)) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }

        } catch (Exception e) {
            log.error("OAuth test failed: {}", e.getMessage(), e);

            result.put("status", "ERROR");
            result.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @PostMapping("/oauth/clear")
    public ResponseEntity<Map<String, String>> clearOAuthToken() {
        log.info("Clearing cached OAuth token");

        oAuthTokenService.clearToken();

        Map<String, String> result = new HashMap<>();
        result.put("status", "SUCCESS");
        result.put("message", "OAuth token cache cleared");

        return ResponseEntity.ok(result);
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, String>> testConfiguration() {
        log.info("Testing configuration");

        Map<String, String> config = new HashMap<>();

        try {
            String token = oAuthTokenService.getAccessToken();
            config.put("status", "SUCCESS");
            config.put("message", "Configuration is valid and OAuth token obtained");
            config.put("token_preview", token.substring(0, Math.min(20, token.length())) + "...");
        } catch (Exception e) {
            config.put("status", "FAILURE");
            config.put("message", "Configuration error: " + e.getMessage());
        }

        return ResponseEntity.ok(config);
    }
}
