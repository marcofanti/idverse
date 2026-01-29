package org.itnaf.idverse.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itnaf.idverse.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

/**
 * Controller for authentication endpoints.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final String authKey;

    /**
     * Authentication endpoint that validates the auth_key and generates a JWT token.
     * If successful, redirects to root URL with jwt_key parameter.
     *
     * @param providedAuthKey The auth_key provided by the caller
     * @return Redirect to root URL with jwt_key, or 403 Forbidden if auth_key is invalid
     */
    @GetMapping("/getAuth")
    public Object getAuth(@RequestParam(value = "auth_key", required = false) String providedAuthKey) {
        log.info("=== Authentication Request Received ===");

        // Validate auth_key parameter
        if (providedAuthKey == null || providedAuthKey.isEmpty()) {
            log.warn("Missing auth_key parameter");
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "error", "Forbidden",
                            "message", "Missing auth_key parameter"
                    ));
        }

        // Check if auth_key matches the configured AUTH_KEY
        if (!authKey.equals(providedAuthKey)) {
            log.warn("Invalid auth_key provided");
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "error", "Forbidden",
                            "message", "Invalid auth_key"
                    ));
        }

        log.info("auth_key validated successfully");

        // Generate JWT token and jwt_key
        AuthService.TokenPair tokenPair = authService.generateTokenPair();

        log.info("Generated token pair, redirecting to root with jwt_key");

        // Redirect to root URL with jwt_key parameter
        RedirectView redirectView = new RedirectView("/");
        redirectView.addStaticAttribute("jwt_key", tokenPair.getJwtKey());

        return redirectView;
    }
}
