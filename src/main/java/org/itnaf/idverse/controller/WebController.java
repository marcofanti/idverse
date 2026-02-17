package org.itnaf.idverse.controller;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itnaf.idverse.client.model.VerificationRequest;
import org.itnaf.idverse.model.VerificationResponse;
import org.itnaf.idverse.service.AuthService;
import org.itnaf.idverse.service.IdVerificationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebController {

    private final IdVerificationService verificationService;
    private final AuthService authService;
    private final Dotenv dotenv;

    private static final String SESSION_JWT_TOKEN = "jwt_token";

    @GetMapping("/")
    public String home(@RequestParam(value = "jwt_key", required = false) String jwtKey,
                      HttpSession session,
                      Model model) {

        // Check authentication
        if (!authenticateRequest(jwtKey, session)) {
            log.warn("Unauthorized access attempt to home page");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied. Please authenticate via /api/getAuth");
        }

        VerificationRequest request = new VerificationRequest();

        // Set default values from .env if available, with fallbacks for required fields
        String phoneCode = dotenv.get("PHONE_CODE");
        request.setPhoneCode(phoneCode != null && !phoneCode.isEmpty() ? phoneCode : "+1");

        String phoneNumber = dotenv.get("PHONE_NUMBER");
        request.setPhoneNumber(phoneNumber != null && !phoneNumber.isEmpty() ? phoneNumber : "9412607454");

        String referenceId = dotenv.get("REFERENCE_ID");
        request.setReferenceId(referenceId != null && !referenceId.isEmpty()
            ? referenceId : "ref-" + System.currentTimeMillis());

        // Optional fields - set transactionId from TRANSACTION env var with random suffix
        String transaction = dotenv.get("TRANSACTION");
        if (transaction != null && !transaction.isEmpty()) {
            request.setTransactionId(appendRandomSuffix(transaction.trim()));
        }

        String name = dotenv.get("NAME");
        if (name != null && !name.isEmpty()) {
            request.setName(name);
        }

        String suppliedFirstName = dotenv.get("SUPPLIED_FIRST_NAME");
        if (suppliedFirstName != null && !suppliedFirstName.isEmpty()) {
            request.setSuppliedFirstName(suppliedFirstName);
        }

        model.addAttribute("verificationRequest", request);
        model.addAttribute("verifications", verificationService.getAllVerifications());
        return "index";
    }

    @PostMapping("/verify")
    public String verify(@Valid @ModelAttribute("verificationRequest") VerificationRequest request,
                        BindingResult bindingResult,
                        HttpSession session,
                        Model model) {

        // Check authentication
        if (!isAuthenticated(session)) {
            log.warn("Unauthorized access attempt to verify endpoint");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied. Please authenticate via /api/getAuth");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("verifications", verificationService.getAllVerifications());
            return "index";
        }

        log.info("Received web form verification request: {}", request);

        VerificationResponse response = verificationService.verify(request);
        model.addAttribute("result", response);

        return "results";
    }

    @GetMapping("/results")
    public String results(@RequestParam(required = false) Long id,
                         HttpSession session,
                         Model model) {

        // Check authentication
        if (!isAuthenticated(session)) {
            log.warn("Unauthorized access attempt to results page");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied. Please authenticate via /api/getAuth");
        }

        if (id != null) {
            verificationService.getVerificationById(id).ifPresent(result -> {
                model.addAttribute("result", result);
            });
        }
        return "results";
    }

    @GetMapping("/history")
    public String history(HttpSession session, Model model) {
        // Check authentication
        if (!isAuthenticated(session)) {
            log.warn("Unauthorized access attempt to history page");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied. Please authenticate via /api/getAuth");
        }

        List<VerificationResponse> verifications = verificationService.getAllVerifications();
        model.addAttribute("verifications", verifications);
        return "history";
    }

    /**
     * Authenticates a request by checking jwt_key parameter or session.
     * If jwt_key is provided, validates it and sets session.
     * Otherwise, checks session for valid JWT token.
     *
     * @param jwtKey JWT key from query parameter (optional)
     * @param session HTTP session
     * @return true if authenticated, false otherwise
     */
    private boolean authenticateRequest(String jwtKey, HttpSession session) {
        // If jwt_key parameter is provided, validate and set session
        if (jwtKey != null && !jwtKey.isEmpty()) {
            log.info("jwt_key parameter provided, validating...");
            String jwtToken = authService.validateAndConsumeKey(jwtKey);

            if (jwtToken != null) {
                // Valid jwt_key - store JWT token in session
                session.setAttribute(SESSION_JWT_TOKEN, jwtToken);
                log.info("Session authenticated with jwt_key");
                return true;
            } else {
                log.warn("Invalid or expired jwt_key");
                return false;
            }
        }

        // No jwt_key parameter - check session
        return isAuthenticated(session);
    }

    /**
     * Checks if the session has a valid JWT token.
     *
     * @param session HTTP session
     * @return true if session has valid JWT token, false otherwise
     */
    private boolean isAuthenticated(HttpSession session) {
        String jwtToken = (String) session.getAttribute(SESSION_JWT_TOKEN);

        if (jwtToken == null) {
            log.debug("No JWT token in session");
            return false;
        }

        // Validate JWT token (checks expiration)
        boolean isValid = authService.validateJwtToken(jwtToken);

        if (!isValid) {
            log.warn("JWT token in session is invalid or expired");
            session.removeAttribute(SESSION_JWT_TOKEN);
            return false;
        }

        log.debug("Session authenticated with valid JWT token");
        return true;
    }

    /**
     * Appends a random suffix to the transaction value.
     * Adds at least 4 random characters, or enough to reach a minimum of 12 total characters.
     */
    private String appendRandomSuffix(String transaction) {
        int currentLength = transaction.length();
        int minLength = 12;

        // Calculate how many random characters needed (minimum 4)
        int randomCharsNeeded = Math.max(4, minLength - currentLength);

        String randomSuffix = generateRandomAlphanumeric(randomCharsNeeded);
        return transaction + "-" + randomSuffix;
    }

    /**
     * Generates a random alphanumeric string of specified length.
     */
    private String generateRandomAlphanumeric(int length) {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
