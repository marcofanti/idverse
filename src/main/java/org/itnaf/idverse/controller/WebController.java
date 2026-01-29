package org.itnaf.idverse.controller;

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

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebController {

    private final IdVerificationService verificationService;
    private final String defaultPhoneCode;
    private final String defaultPhoneNumber;
    private final String defaultTransaction;
    private final String defaultName;
    private final String defaultSuppliedFirstName;

    @GetMapping("/")
    public String home(Model model) {
        VerificationRequest request = new VerificationRequest();

        // Set default values from .env if available
        if (defaultPhoneCode != null && !defaultPhoneCode.isEmpty()) {
            request.setPhoneCode(defaultPhoneCode);
        }
        if (defaultPhoneNumber != null && !defaultPhoneNumber.isEmpty()) {
            request.setPhoneNumber(defaultPhoneNumber);
        }
        if (defaultTransaction != null && !defaultTransaction.isEmpty()) {
            request.setTransaction(defaultTransaction);
        }
        if (defaultName != null && !defaultName.isEmpty()) {
            request.setName(defaultName);
        }
        if (defaultSuppliedFirstName != null && !defaultSuppliedFirstName.isEmpty()) {
            request.setSuppliedFirstName(defaultSuppliedFirstName);
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
}
