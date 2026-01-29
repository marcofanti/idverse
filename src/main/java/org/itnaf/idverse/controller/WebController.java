package org.itnaf.idverse.controller;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itnaf.idverse.model.VerificationRequest;
import org.itnaf.idverse.model.VerificationResponse;
import org.itnaf.idverse.service.IdVerificationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.SecureRandom;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebController {

    private final IdVerificationService verificationService;
    private final Dotenv dotenv;

    @GetMapping("/")
    public String home(Model model) {
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
                        Model model) {

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
    public String results(@RequestParam(required = false) Long id, Model model) {
        if (id != null) {
            verificationService.getVerificationById(id).ifPresent(result -> {
                model.addAttribute("result", result);
            });
        }
        return "results";
    }

    @GetMapping("/history")
    public String history(Model model) {
        List<VerificationResponse> verifications = verificationService.getAllVerifications();
        model.addAttribute("verifications", verifications);
        return "history";
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
